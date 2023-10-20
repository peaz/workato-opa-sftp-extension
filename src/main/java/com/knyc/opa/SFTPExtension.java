/*
 * Copyright (c) 2023 Ken Ng, Inc. All rights reserved.
 */

package com.knyc.opa;

import org.springframework.context.Lifecycle;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;

import org.apache.commons.io.IOUtils;

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

    private static String INVALID_CONNECTION_ERROR_MSG = "SFTP Extention Connection is not valid. Please check and reconnect the connector.";
    private static String INVALID_ARCHIVE_DIRECTORY = "Invalid archive directory defined. Please check the connector configurations and make sure the archive directory exists.";
    private static String INVALID_DIRECTORY = "Invalid directory defined. Please check the connector configurations and make sure the directory exists.";

    private JSch jsch;    
    private Session jschSession;

    @RequestMapping(path = "/connect", method = RequestMethod.POST, produces = "application/json")
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg, e);
        }        
        
        if (errorMsg.isEmpty()) {
            String successMsg = "successfully connected to remote SFTP host [" + this._remoteHost + ":" + this._remotePort + "]"; 
            responseData.put("status","success");
            responseData.put("message",successMsg);
        } else {
            responseData = createErrorMsg(errorMsg);
        }        
        
        return responseData;
    }

    @RequestMapping(path = "/uploadFileContent", method = (RequestMethod.POST), produces = "application/json")
    @ResponseBody
    public Map<String, Object> uploadFileContent(@RequestBody Map<String, Object> body) throws Exception {        
        
        if (!isValidDirectory((String) body.get("fileDirectory"))){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, INVALID_DIRECTORY); 
        }

        Map<String, Object> responseData = new HashMap<String, Object>();
        String fileContentinBase64 = (String) body.get("fileContent");
        String fullFilePath = (String) body.get("fileDirectory") + (String) body.get("fileName");

        if (this._channelSftp != null) {
            responseData = uploadFiletoSFTP(fileContentinBase64, fullFilePath);
        } else {
            responseData = createErrorMsg(INVALID_CONNECTION_ERROR_MSG);
        }

        return responseData;
        
    }

    @RequestMapping(path = "/downloadFileContent", method = (RequestMethod.POST), produces = "application/json")
    @ResponseBody
    public Map<String, Object> downloadFileContent(@RequestBody Map<String, Object> body) throws Exception {        
        
        if (!isValidDirectory((String) body.get("fileDirectory"))){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, INVALID_DIRECTORY); 
        }

        Map<String, Object> downloadResponseData = new HashMap<String, Object>();
        Map<String, Object> postReadResponseData = new HashMap<String, Object>();
        String fullFilePath = (String) body.get("fileDirectory") + (String) body.get("fileName");
        String postRead = (String) body.get("postRead");        
        String archiveFilePath = (String) body.get("archiveDirectory") + (String) body.get("fileName");

        LOG.info("downloadFileContent executed. postRead:" + postRead + " | fullFilePath:" + fullFilePath + " | archiveFilePath:" + archiveFilePath);

        if (this._channelSftp != null) {            
            downloadResponseData = downloadFilefromSFTP(fullFilePath);
        } else {
            downloadResponseData = createErrorMsg(INVALID_CONNECTION_ERROR_MSG);
        }

        if (postRead.equals("archive")){
            if (!isValidDirectory((String) body.get("archiveDirectory"))){
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, INVALID_ARCHIVE_DIRECTORY); 
            }
            postReadResponseData = renameFileAtSFTP(fullFilePath, archiveFilePath);
            if (postReadResponseData.get("status") == "success") {
                downloadResponseData.put("achiveFilePath", archiveFilePath);
            }
            else {
                downloadResponseData = postReadResponseData;
            }
        } else if (postRead.equals("delete")) {            
            postReadResponseData = removeFileAtSFTP(fullFilePath);
            if (postReadResponseData.get("status") == "success") {
                downloadResponseData.put("isDeleted", true);
            }
            else {
                downloadResponseData = postReadResponseData;
            }
        } else {
            //defaults to renaming to .archive if invalid operation.
            postReadResponseData = renameFileAtSFTP(fullFilePath, fullFilePath + ".archive");
            if (postReadResponseData.get("status") == "success") {
                downloadResponseData.put("achiveFilePath", fullFilePath + ".archive");
            }
            else {
                downloadResponseData = postReadResponseData;
            }            
        }
        return downloadResponseData;        
    }

    @RequestMapping(path = "/getFileAttrs", method = (RequestMethod.POST), produces = "application/json")
    @ResponseBody
    public Map<String, Object> getFileAttrs(@RequestBody Map<String, Object> body) throws Exception {        
        
        Map<String, Object> responseData = new HashMap<String, Object>();
        String fullPath = (String) body.get("fullPath");        

        if (this._channelSftp != null) {            
            responseData = getFileAttrsfromSFTP(fullPath);
        } else {
            responseData = createErrorMsg(INVALID_CONNECTION_ERROR_MSG);
        }
        return responseData;
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

    private Map<String, Object> createErrorMsg(String errorMsg){
        Map<String, Object> responseData = new HashMap<String, Object>();
        responseData.put("status","error");
        responseData.put("message",errorMsg);
        LOG.error(errorMsg);
        return responseData;
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

    private Map<String, Object> uploadFiletoSFTP(String fileContentinBase64, String fullFilePath) throws Exception {
        Map<String, Object> responseData = new HashMap<String, Object>();
        String errorMsg = "";        
        try {
            
            //convert fileContentinBase64 to byteArray
            byte[] decodedBytes = Base64.getDecoder().decode(fileContentinBase64);
            
            //create fileContent byteArrayInputStream
            ByteArrayInputStream fileContent = new ByteArrayInputStream(decodedBytes);

            // transfer file from local to remote server, always OVERWRITE mode
            this._channelSftp.put(fileContent, fullFilePath, ChannelSftp.OVERWRITE);            
        } catch (Exception e) {
            errorMsg = e.getMessage();
            e.printStackTrace();            
        }
        
        if (errorMsg.isEmpty()) {
            String successMsg = "Completed file upload for file " + fullFilePath;
            responseData.put("status","success");
            responseData.put("message",successMsg);
            LOG.info(successMsg);
        } else {
            responseData = createErrorMsg(errorMsg);
        }   
        return responseData;
      }

    private Map<String, Object> downloadFilefromSFTP(String fullFilePath) throws Exception {
        Map<String, Object> responseData = new HashMap<String, Object>();
        String errorMsg = "";
        String fileContentinBase64 = "";
        byte[] fileContentinByteArray = null;
        
        try {            
            InputStream fileContentStream = null;

            // download file from remote server
            fileContentStream = this._channelSftp.get(fullFilePath);

            //write fileContentStream into byteArray
            fileContentinByteArray = IOUtils.toByteArray(fileContentStream);

            //convert fileContentinByteArray to Base64
            fileContentinBase64 = Base64.getEncoder().encodeToString(fileContentinByteArray);

        } catch (Exception e) {
            errorMsg = e.getMessage();
            e.printStackTrace();
        }
        
        if (errorMsg.isEmpty()) {
            String successMsg = "Completed file download for " + fullFilePath;
            responseData.put("status","success");
            responseData.put("message",successMsg);
            responseData.put("fileContentinBase64",fileContentinBase64);
            LOG.info(successMsg);
        } else {
            responseData = createErrorMsg(errorMsg);
        }   
        return responseData;
      }

    private boolean isValidDirectory(String directoryPath) throws Exception {
        try {    
            // use lstat to check if path is a directory
            SftpATTRS fileAttrs = this._channelSftp.lstat(directoryPath);            
            return fileAttrs.isDir();
        }
        catch (Exception e) {
            // if errored out, then it's definitely invalid
            return false;
        }
    }
    
    private Map<String, Object> getFileAttrsfromSFTP(String fullPath) throws Exception {
        Map<String, Object> responseData = new HashMap<String, Object>();
        String errorMsg = "";
        long sizeInBytes = 0;
        int modifiedTimestamp = 0;
        boolean isDir = false;
        
        try {            
            // get file size from remote server
            SftpATTRS pathAttrs = this._channelSftp.lstat(fullPath);
            sizeInBytes = pathAttrs.getSize();
            modifiedTimestamp = pathAttrs.getMTime();
            isDir = pathAttrs.isDir();
            
        } catch (Exception e) {
            errorMsg = e.getMessage();
            e.printStackTrace();
        }
        
        if (errorMsg.isEmpty()) {
            responseData.put("status","success");
            responseData.put("sizeInBytes",sizeInBytes);
            responseData.put("modifiedTimestamp",modifiedTimestamp);
            responseData.put("isDir",isDir);
            String successMsg = "Returned path attributes for " + fullPath;
            LOG.info(successMsg);
        } else {
            responseData = createErrorMsg(errorMsg);
        }   
        return responseData;
    }

    private Map<String, Object> renameFileAtSFTP(String oldFilePath, String newFilePath) throws Exception {
        Map<String, Object> responseData = new HashMap<String, Object>();
        String errorMsg = "";
        
        try {            
            this._channelSftp.rename(oldFilePath,newFilePath);
            
        } catch (Exception e) {
            errorMsg = e.getMessage();
            e.printStackTrace();
        }
        
        if (errorMsg.isEmpty()) {
            String successMsg = "Renamed file from " + oldFilePath + " to " + newFilePath;
            responseData.put("status","success");
            responseData.put("newFilePath",newFilePath);
            LOG.info(successMsg);
        } else {
            responseData = createErrorMsg(errorMsg);
        }   
        return responseData;
    }

    private Map<String, Object> removeFileAtSFTP(String fullFilePath) throws Exception {
        Map<String, Object> responseData = new HashMap<String, Object>();
        String errorMsg = "";
        
        try {            
            this._channelSftp.rm(fullFilePath);
            
        } catch (Exception e) {
            errorMsg = e.getMessage();
            e.printStackTrace();
        }
        
        if (errorMsg.isEmpty()) {
            String successMsg = "Removed file " + fullFilePath;
            responseData.put("status","success");
            LOG.info(successMsg);
        } else {
            responseData = createErrorMsg(errorMsg);
        }   
        return responseData;
    }
}
