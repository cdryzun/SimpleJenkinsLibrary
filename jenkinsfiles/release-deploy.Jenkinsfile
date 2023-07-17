#!groovy

@Library('jenkins-library@master') _

//func from shareibrary
def utils = new org.devops.utils()

// pipeline
pipeline {

    //确认使用主机/节点机
    agent any /*{ 
        node { label ' master'} 
    }*/

    options{
        timeout(time: 30, unit: 'MINUTES') // pipeline 超时时间
        disableConcurrentBuilds()  // 关闭并发构建
        // retry(3) // 是否开启构建自动重试
        buildDiscarder(logRotator(numToKeepStr: '30')) // 设置保留的 pipeline log 数量
    }

    parameters {
        // 复选框 选择式构建
        string(name: 'srcUrl', defaultValue: 'https://www.github.com', description: '发布制品所使用的仓库地址')
        string(name: 'branchName', defaultValue: 'vx.x.x', description: '发布制品所使用的仓库分支')
        choice(choices: ['M2','NPM'], description: '工程构建工具选择', name: 'buildType')
        string(name: 'buildShell', defaultValue: 'mvn clean package -Dmaven.test.skip', description: '发布制品所使用的构建命令')
    }

    stages {
        stage('Clean') {
            steps {
                script{
                    // tools.PrintMes("清除工作空间","green")
                    cleanWs()
                    utils.GetBuildUser()
                    currentBuild.description = "Manual trigger by ${userName} ${branchName}"
                    branchName = params.branchName
                    buildType = params.buildType
                    buildShell = params.buildShell
                    srcUrl = params.srcUrl
                }
            }
        }
        stage("CheckOut"){
            steps{
                script{           
                    checkout([$class: 'GitSCM', branches: [[name: "${branchName}"]], 
                                      doGenerateSubmoduleConfigurations: false, 
                                      extensions: [], 
                                      submoduleCfg: [], 
                                      userRemoteConfigs: [[credentialsId: 'clone-auth', url: "${srcUrl}"]]])

                }
            }
        }

        stage('Build') {
            steps{
                script {
                    buildHome = tool buildType
                    sh """
                        export TOOL_HOME=${buildHome} \
                        && export PATH=\${TOOL_HOME}/bin:\$PATH \
                        && echo "${buildShell}"|bash
                    """
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar,dist/', fingerprint: true
        }
    }
}