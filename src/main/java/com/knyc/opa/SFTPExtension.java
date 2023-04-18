/*
 * Copyright (c) 2018 Ken Ng, Inc. All rights reserved.
 */

package com.knyc.opa;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
// import org.springframework.web.multipart.MultipartFile;
// import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.context.Lifecycle;
// import org.springframework.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.*;

// import java.io.ByteArrayInputStream;
// import java.util.Collection;
import java.util.HashMap;
// import java.util.HashSet;
import java.util.Map;

@Controller
public class SFTPExtension implements Lifecycle{

    private static final Logger LOG = LoggerFactory.getLogger(SFTPExtension.class);  
    private ChannelSftp _channelSftp;
    private String _remoteHost;
    private String _username;
    private String _password;
    private String _knownHostsPath;
    private int _remotePort;
    private int _sessionTimeout;
    private int _channelTimeout;

    private JSch jsch;    
    private Session jschSession;

    @RequestMapping(path = "/connect", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> connect(@RequestBody Map<String, Object> body) throws Exception {
        
        Map<String, Object> responseData = new HashMap<String, Object>();

        this._remoteHost = (String) body.get("remoteHost");
        this._username = (String) body.get("username");
        this._password = (String) body.get("password");
        this._knownHostsPath = (String) body.get("knownHostPath");
        this._remotePort = Integer.parseInt((String) body.get("remotePort"));
        this._sessionTimeout = Integer.parseInt((String) body.get("sessionTimeout"));
        this._channelTimeout = Integer.parseInt((String) body.get("channelTimeout"));
        String errorMsg = "";
        
        try {
            this._channelSftp = setupJsch(this._remoteHost, this._username, this._password, this._knownHostsPath, this._remotePort, this._sessionTimeout);
            this._channelSftp.connect(this._channelTimeout);
            LOG.info("SFTP extension successfully connected to remote SFTP host [" + this._remoteHost + ":" + this._remotePort + "]");
        } catch (Exception e) {
            errorMsg = e.getMessage();
            e.printStackTrace();    
            // throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);  
        }
        
        if (errorMsg.isEmpty()) {
            String successMsg = "successfully connected to remote SFTP host [" + this._remoteHost + ":" + this._remotePort + "]"; 
            responseData.put("status","success");
            responseData.put("message",successMsg);
        } else {            
            responseData.put("status","error");
            responseData.put("message",errorMsg);
        }        
        
        return responseData;
    }

    @RequestMapping(path = "/uploadFileContent", method = (RequestMethod.POST))
    @ResponseBody
    public Map<String, Object> uploadFileContent(@RequestParam Map<String, Object> body) throws Exception {        
        
        String fileContent = (String) body.get("fileContent");
        String remotePath = (String) body.get("remotePath");
        String filename = (String) body.get("filename");

        return uploadFiletoSFTP(fileContent, remotePath, filename);
    }

    public void stop() {
        if (this._channelSftp.isConnected()) {
          this._channelSftp.disconnect();
          LOG.info("SFTP extension closed with channel exit status:" + this._channelSftp.getExitStatus());
        } 
        this.jschSession.disconnect();
        LOG.info("SFTP extension closed with Session Running Status:" + this.jschSession.isConnected());
      }
      
    public boolean isRunning() {
    return true;
    }
    
    public void start() {
        LOG.info("Starting SFTP Extension");
    }

    private ChannelSftp setupJsch(String remoteHost, String username, String password, String knownHostsPath, int remotePort, int sessionTimeout) throws Exception {
        this.jsch = new JSch();
        if (knownHostsPath != null)
          this.jsch.setKnownHosts(knownHostsPath); 
        this.jschSession = this.jsch.getSession(username, remoteHost, remotePort);
        this.jschSession.setPassword(password);
        // this.jsch.addIdentity(_privatekey);
        this.jschSession.connect(sessionTimeout);
        this.jschSession.sendKeepAliveMsg();
        return (ChannelSftp)this.jschSession.openChannel("sftp");
      }

      private Map<String, Object> uploadFiletoSFTP(String fileContent, String remotePath, String filename) throws Exception {
        Map<String, Object> responseData = new HashMap<String, Object>();
        String errorMsg = "";
        try {            
            //create source byteArrayInputStream
            //ByteArrayInputStream sourceContent = new ByteArrayInputStream(fileContent);
            String sourceContent = fileContent;

            // transfer file from local to remote server, always OVERWRITE mode
            this._channelSftp.put(sourceContent, remotePath + "/" + filename, ChannelSftp.OVERWRITE);
            
        } catch (Exception e) {
            errorMsg = e.getMessage();
            e.printStackTrace();            
        }
        
        if (errorMsg.isEmpty()) {
            String successMsg = "Completed file upload for file name " + filename + ". Current Connection status: " + this._channelSftp.isConnected();
            responseData.put("status","success");
            responseData.put("message",successMsg);
            LOG.info(successMsg);
        } else {
            responseData.put("status","error");
            responseData.put("message",errorMsg);
            LOG.error(errorMsg);
        }   
        return responseData;
      }

}
