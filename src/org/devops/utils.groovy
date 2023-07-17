package org.devops

// 代码 build , 依赖 Jenkins 页面配置对应工具
def Build(buildType,buildShell,buildPath='null'){
    def buildTools = ["mvn":"M2","ant":"ANT","gradle":"GRADLE","npm":"NPM"]
    def buildBasePath = buildPath
    
    println("当前选择的构建类型为 ${buildType}")
    buildHome = tool buildTools[buildType]
    
    if ("${buildType}" == "npm"){
        sh  """ 
            if [ ${buildBasePath} != 'null' ];then
                cd ${buildBasePath}
            fi;
            
            export NODE_HOME=${buildHome} \
            && export PATH=\$NODE_HOME/bin:\$PATH \
            && ${buildHome}/bin/npm install \
            && ${buildHome}/bin/${buildType} ${buildShell}
        """
    } else {
        sh """
            if [ ${buildBasePath} != 'null' ];then
                cd ${buildBasePath}
            fi;
            ${buildHome}/bin/${buildType}  ${buildShell}
        """
    }
}

// 通用触发器生成
def GenericTrigger(branchName){
    if ("${branchName}" != "null"){
        properties([
            pipelineTriggers([
            [$class: 'GenericTrigger',
            genericVariables: [
            [key: 'branch', value: '$.ref'],
            [key: 'userName', value: '$.user_name'],
            [key: 'srcUrl', value: '$.project.git_http_url'],
            [key: 'object_kind', value: '$.object_kind'],
            [key: 'before', value: '$.before'],
            [key: 'after', value: '$.after'],
            [key: 'projectId', value: '$.project_id'],
            [key: 'commitSha', value: '$.checkout_sha'],
            [key: 'repoName', value: '$.project.name'],
            [key: 'description', value: '$.project.description']
            ],
            genericHeaderVariables: [
                [key: "requestParameterName", regexpFilter: ""]
            ],
            genericRequestVariables: [
                [key: "runOpts", regexpFilter: ""],
                [key: "buildType", regexpFilter: ""]
            ],
            causeString: 'Triggered on $branch',
            token: "${JOB_NAME}",
            printContributedVariables: true,
            printPostContent: true,
            silentResponse: true,
            regexpFilterText: '$object_kind $before $after $branch',
            regexpFilterExpression: "^push\\s(?!0{40}).{40}\\s(?!0{40}).{40}\\s\\w+/\\w+/(dev|sit|uat|prod)\$"
            // regexpFilterExpression: "^push\\s(?!0{40}).{40}\\s(?!0{40}).{40}\\s\\w+/\\w+/(${branchName})\$"
            ]
            ])
        ])
    }
}

def GetBuildUser(){
  wrap([$class: 'BuildUser']) {
      env.userName = env.BUILD_USER_ID
  }
}
