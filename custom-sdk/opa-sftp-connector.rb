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
      ).headers('X-Workato-Connector': 'enforce')
  },
  
  actions: {
    getFileAttrs: {
      title: 'Get file or directory attributes',
      description: 'Get size, modified time and isDir flag',

      input_fields: ->  {[
       {
          name: 'fullPath',
          label: 'Full path to download',
          optional: false,
          hint: 'Please provide format like /test/download/sample.txt'    
        }
      ]},      
      execute: ->(connection, input) {
        post("http://localhost/ext/#{connection['profileName']}/getFileAttrs",input)
        .headers('X-Workato-Connector': 'enforce')
        .after_response do |code, body, header|
          if body["status"] == "error"
            error("error: #{body["message"]}")
          else
            result = {
              "status": body["status"],
              "isDir": body["status"],
              "sizeInBytes": body["sizeInBytes"],
              "modifiedTimestamp": Time.at(body["modifiedTimestamp"])
            }
          end
        end
      },
      output_fields: -> {[
        {name: 'status', type: 'string'},
        {name: 'isDir', type: 'boolean'},
        {name: 'sizeInBytes', type: 'number'},
        {name: 'modifiedTimestamp', type: 'timestamp'}
      ]}
    },
    uploadToSFTP: {
      title: 'Upload file',
      description: 'Uploads a file to an SFTP server',

      input_fields: ->  {[
        {
          name: 'fileContent',
          label: 'File content to be uploaded',
          optional: false,
          hint: 'The content of the file to be uploaded. Note that file content will be auto-converted to Base64 if file content is not in plaintext',
        },
        {
          name: 'fileDirectory',
          label: 'Full directory path of file to download',
          optional: false,
          hint: 'Please provide full directory path. Example "/test/download/"'
        },
        {
          name: 'fileName',
          label: 'File name to download',
          optional: false,
          hint: 'Please provide name of the file to download. Example "file.txt"'
        }
        
      ]},
      execute: ->(connection, input) {
        post("http://localhost/ext/#{connection['profileName']}/uploadFileContent",input)
        .headers('X-Workato-Connector': 'enforce')
        .after_response do |code, body, header|
          if body["status"] == "error"
            error("error: #{body["message"]}")
          else
            body
          end
        end
      },
      output_fields: -> {[
        {name: 'status', type: 'string'},
        {name: 'message', type: 'string'}
      ]}
    },
    downloadFromSFTP: {
      title: 'Download file',
      description: 'Downlods a remote file from an SFTP server',

      input_fields: ->  {[    
        {
          name: 'fileDirectory',
          label: 'Full directory path of file to download',
          optional: false,
          hint: 'Please provide full directory path. Example "/test/download/"'    
        },
        {
          name: 'fileName',
          label: 'File name to download',
          optional: false,
          hint: 'Please provide name of the file to download. Example "file.txt"'    
        },
        {
          name: 'postRead',
          control_type: 'select',
          pick_list: 'PostReadOptions',
          optional: false,
          label: 'Post read action to delete or archive',
        },
        {
          name: 'archiveDirectory',
          label: 'Archive Directory',
          ngIf: 'input.postRead == "archive"',
          sticky: true,
          hint: 'Provide the full archive directory path. Example "/test/archive/"' 
        }        
      ]},      
      execute: ->(connection, input) {
        post("http://localhost/ext/#{connection['profileName']}/downloadFileContent",input)
        .headers('X-Workato-Connector': 'enforce')
        .after_response do |code, body, header|
          if body["status"] == "error"
            error("error: #{body["message"]}")
          else
            body["fileContent"] = body["fileContentinBase64"].decode_base64
            body
          end          
        end
      },
      output_fields: -> {[
        {name: 'status', type: 'string'},
        {name: 'message', type: 'string'},
        {name: 'fileContentInBase64', label: 'File Content (base64 encoded)', type: 'string', optional: true},
        {name: 'fileContent', type: 'string'}
        ]
      }
    }  
  }
} 