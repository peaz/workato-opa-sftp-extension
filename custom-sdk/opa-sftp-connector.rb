{
  title: 'On-Prem SFTP Connector',
  secure_tunnel: true,

  pick_lists: {
      PostReadOptions: lambda do |connection|
        [
          ["Archive","archive"],
          ["Delete","delete"]
       ]
      end,

    },
  connection: {
   
   fields: [
     { name: 'profileName', label: 'profile', hint: 'Please contact OPA admin', optional: false },
     { name: 'remoteHost', label: 'SFTP Host', hint: 'SFTP Host', type: "integer", optional: false },
     { name: 'remotePort', label: 'SFTP Port', hint: 'SFTP Port', type: "integer", default: "22", optional: false },
     { name: 'sessionTimeout', label: 'Session Timeout', hint: 'Session Timeout in milliseconds', type: "integer", default: "500", optional: false },
     { name: 'channelTimeout', label: 'Channel Timeout', hint: 'Channel Timeout in milliseconds', type: "integer", default: "2000", optional: false },
     { name: 'username', label: 'User Name', hint: 'SFTP User Name', optional: false },
     { name: 'password', label: 'Password', hint: 'SFTP Password', control_type: "password", optional: false},
     { name: 'knownHostPath', label: 'Known Host File Path', hint: '/users/<<userid>>/.ssh/known_hosts', optional: false}     
   ],
    authorization: { type: 'none'},
    apply: ->() {
      headers('X-Workato-Connector': 'enforce')
   }
 },


  test: ->(connection) {
   post("http://localhost/ext/#{connection['profileName']}/connect",
      remoteHost: connection['remoteHost'],
      remotePort: connection['remotePort'],
      sessionTimeout: connection['sessionTimeout'],
      channelTimeout: connection['channelTimeout'],
      username: connection['username'],
      password: connection['password'],
      knownHostPath: connection['knownHostPath'],
      
      ).headers('X-Workato-Connector': 'enforce').after_error_response(500) do |code, body, header, message|
      error("#{code}: #{body}")
   end

  },
  
  actions: {
    uploadToSFTP: {
      title: 'Upload local file to SFTP Server',
      description: 'Reads OPA local file and sends to SFTP remote directory',

      input_fields: ->  {[
        {
          name: 'fileContent',
          label: 'File Content',
          optional: false,
          hint: 'File content' 
        },
       {
          name: 'remotePath',
          label: 'Remote Folder Path',
          optional: false
   
        },
         {
          name: 'filename',
          label: 'File name',
          optional: false  
        }
        
      ]},
      output_fields: -> { [{name: 'status', name: 'message' }] },
  
      execute: ->(connection, input) {
        post("http://localhost/ext/#{connection['profileName']}/uploadFileContent",input).
        headers('X-Workato-Connector': 'enforce').
        params(version: "2015-12-15").
        request_format_multipart_form.
        payload(
          file: [input['fileContant'], 'text/plain'],
          filename: input['filename'],
          remotePath: input['remotePath']
        )
      }
    },
    # downloadFromSFTP: {
    #   title: 'Download SFTP remote file to local directory',
    #   description: 'Reads SFTP remote file and write to the local directory',

    #   input_fields: ->  {[
    
    #    {
    #       name: 'remoteFolder',
    #       label: 'Remote Folder Path',
    #       optional: false,
    #       hint: 'Please provide format like path /test/out/' 
   
    #     },
    #       {
    #       name: 'fileName',
    #       label: 'Remote File Name',
    #       optional: false
   
    #     },
    #      {
    #       name: 'localFolder',
    #       label: 'Local Folder Path',
    #       optional: false,
    #       hint: 'Please provide format like path /test/in/' 
    #     },
    #      {
    #       name: 'post_read',
    #       control_type: 'select',
    #       pick_list: 'PostReadOptions',
    #       optional: false,
    #       label: 'Action required Delete or Archive',
    #     },
    #     {
    #       name: 'moveTo',
    #       label: 'Archive Folder',
    #       ngIf: 'input.post_read == "archive"',
    #       sticky: true,
    #       hint: 'Provide the complete archive folder path  like path /test/archive/' 
    #     }
        
    #   ]},
    #   output_fields: -> { [{name: 'status' }] },
  
   

    #   execute: ->(connection, input) {
    #     post("http://localhost/ext/#{connection['profileName']}/downloadFileContent",input).headers('X-Workato-Connector': 'enforce')
    #   }
    # }
  },


} 