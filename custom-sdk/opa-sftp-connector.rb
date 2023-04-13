{
  title: 'On-Premises SFTP Connector',
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
     { name: 'remoteHost', label: 'SFTP Host', hint: 'SFTP Host', optional: false },
     { name: 'remotePort', label: 'SFTP Port', hint: 'SFTP Port', default: "22", optional: false },
     { name: 'username', label: 'User Name', hint: 'SFTP User Name', optional: false },
     { name: 'password', label: 'Password', ngIf: 'input.authtype == "pwd"', control_type: "password", sticky: true},
     { name: 'knownHostFile', label: 'Known Host File Path', hint: '/users/<<userid>>/.ssh/known_hosts', optional: false}     
   ],
    authorization: { type: 'none'},
    apply: ->() {
      headers('X-Workato-Connector': 'enforce')
   }
 },


  test: ->(connection) {
   post("http://localhost/ext/#{connection['profileName']}/connect",
      remoteHost: connection['remoteHost'],
      username: connection['username'],
      password: connection['password'],
      knownHostFile: connection['knownHostFile'],
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
   
        },
        {
          name: 'overwrite',
          label: 'Overwrite if file already exists?',
          control_type: 'checkbox',
          type: 'boolean',
          optional: false,
          sticky: false,
          hint: 'Please provide format like path /test/out/' 
        }
        
      ]},
      output_fields: -> { [{name: 'status', name: 'message' }] },
  
      execute: ->(connection, input) {
        post("http://localhost/ext/#{connection['profile']}/uploadFileToRemote",input).headers('X-Workato-Connector': 'enforce')
      }
    },
    downloadFromSFTP: {
      title: 'Download SFTP remote file to local directory',
      description: 'Reads SFTP remote file and write to the local directory',

      input_fields: ->  {[
    
       {
          name: 'remoteFolder',
          label: 'Remote Folder Path',
          optional: false,
          hint: 'Please provide format like path /test/out/' 
   
        },
          {
          name: 'fileName',
          label: 'Remote File Name',
          optional: false
   
        },
         {
          name: 'localFolder',
          label: 'Local Folder Path',
          optional: false,
          hint: 'Please provide format like path /test/in/' 
        },
         {
          name: 'post_read',
          control_type: 'select',
          pick_list: 'PostReadOptions',
          optional: false,
          label: 'Action required Delete or Archive',
        },
        {
          name: 'moveTo',
          label: 'Archive Folder',
          ngIf: 'input.post_read == "archive"',
          sticky: true,
          hint: 'Provide the complete archive folder path  like path /test/archive/' 
        }
        
      ]},
      output_fields: -> { [{name: 'status' }] },
  
   

      execute: ->(connection, input) {
        post("http://localhost/ext/#{connection['profile']}/downLoadFileFromRemote",input).headers('X-Workato-Connector': 'enforce')
      }
    }
  },


}