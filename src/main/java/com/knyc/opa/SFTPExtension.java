/*
 * Copyright (c) 2018 Ken Ng, Inc. All rights reserved.
 */

package com.knyc.opa;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jcraft.jsch.*;
import java.util.Properties;

import javax.inject.Inject;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Controller
public class SFTPExtension {

    @Inject
    private Environment env;

    @RequestMapping(path = "/connect", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> connect(@RequestBody Map<String, Object> body) throws Exception {
        
        Map<String, Object> responseData = new HashMap<String, Object>();

        String REMOTE_HOST = (String) body.get("remoteHost");
        String USERNAME = (String) body.get("username");
        String PASSWORD = (String) body.get("password");
        String KNOWN_HOSTS_PATH = (String) body.get("knownHostPath");
        int REMOTE_PORT = Integer.parseInt((String) body.get("remotePort"));
        int SESSION_TIMEOUT = Integer.parseInt((String) body.get("sessionTimeout"));
        int CHANNEL_TIMEOUT = Integer.parseInt((String) body.get("channelTimeout"));
        String errorMsg = "";
        
        Session jschSession = null;
        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOSTS_PATH);
            jschSession = jsch.getSession(USERNAME, REMOTE_HOST, REMOTE_PORT);
            jschSession.setPassword(PASSWORD);

            jschSession.connect(SESSION_TIMEOUT);
            Channel sftp = jschSession.openChannel("sftp");
            sftp.connect(CHANNEL_TIMEOUT);

            ChannelSftp channelSftp = (ChannelSftp) sftp;
            channelSftp.exit();
            
        } catch (Exception e) {
            errorMsg = e.toString();
            e.printStackTrace();
        }
        
        if (errorMsg.isEmpty()) {
            responseData.put("status","success");
            responseData.put("message","successfully connected to remote SFTP host");
        } else {
            responseData.put("status","error");
            responseData.put("message",errorMsg);
        }        
        
        return responseData;
    }



    @RequestMapping(path = "/uploadFileContent", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> uploadFileContent(@RequestBody Map<String, Object> body) throws Exception {
        
        Map<String, Object> responseData = new HashMap<String, Object>();

        String REMOTE_HOST = (String) body.get("remoteHost");
        String USERNAME = (String) body.get("username");
        String PASSWORD = (String) body.get("password");
        String KNOWN_HOSTS_PATH = (String) body.get("knownHostPath");
        int REMOTE_PORT = (int) body.get("remotePort");
        int SESSION_TIMEOUT = (int) body.get("sessionTimeout");
        int CHANNEL_TIMEOUT = (int) body.get("channelTimeout");
        byte[] FILE_CONTENT = (byte[]) body.get("fileContent");

        String REMOTE_PATH = (String) body.get("remotePath");
        String FILENAME = (String) body.get("filename");

        String errorMsg = "";
        
        Session jschSession = null;

        try {
            JSch jsch = new JSch();
            jsch.setKnownHosts(KNOWN_HOSTS_PATH);
            jschSession = jsch.getSession(USERNAME, REMOTE_HOST, REMOTE_PORT);
            jschSession.setPassword(PASSWORD);

            jschSession.connect(SESSION_TIMEOUT);
            Channel sftp = jschSession.openChannel("sftp");
            sftp.connect(CHANNEL_TIMEOUT);

            ChannelSftp channelSftp = (ChannelSftp) sftp;

            //create source byteArrayInputStream
            ByteArrayInputStream sourceContent = new ByteArrayInputStream(FILE_CONTENT);

            // transfer file from local to remote server
            channelSftp.put(sourceContent, REMOTE_PATH + FILENAME);

            // download file from remote server to local
            // channelSftp.get(remoteFile, localFile);

            channelSftp.exit();
            
        } catch (Exception e) {
            errorMsg = e.toString();
            e.printStackTrace();
        }
        
        if (errorMsg.isEmpty()) {
            responseData.put("status","success");
            responseData.put("message","email sent successfully");
        } else {
            responseData.put("status","error");
            responseData.put("message",errorMsg);
        }        
        
        return responseData;
    }

}
