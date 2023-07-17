#!groovy

@Library('jenkins-library@master') _

//func from shareibrary
def utils = new org.devops.utils()

//env
String runOpts = "${env.runOpts}"
String buildType = "${env.buildType}"
String srcUrl = "${env.srcUrl}"
String branchName = "${env.branchName}"

if ("${runOpts}" == "GitlabPush"){
    branchName = branch - "refs/heads/"
    env.branchName = "${branchName}"
    currentBuild.description = "Webhook trigger by ${userName} ${branchName}"
    env.runOpts = "GitlabPush"
} else {
    // 定义 非 webhook 触发运行 pipeline 所需要的 环境变量
    srcUrl = "https://gitlab-ee.treesir.pub/ci-cd/vue-blog-backend.git"
    buildType = "mvn"
}

utils.GenericTrigger(branchName)

// pipeline
pipeline {

    //确认使用主机/节点机
    agent any /*{ 
        node { label ' master'} 
    }*/

    options{
        timeout(time: 30, unit: 'MINUTES') // pipeline 超时时间
        disableConcurrentBuilds()  // 关闭并发构建
        // retry(3) # 是否开启构建自动重试
        // buildDiscarder(logRotator(numToKeepStr: '15')) # 设置保留的 pipeline log 数量
    }

    environment{
        POM_FILE_PATH='pom.xml'  // pom.xml 文件地址
        DEV_SSH_KEY = credentials('node31-ssh')  // DEV 环境 server
        DEV_SSH_USER = 'root' // DEV ssh user
        DEV_SSH_HOST = '192.168.8.27' // DEV ssh user
        DEV_SSH_PORT = '22' // DEV ssh user
        SSH_OPTS = '-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null'
    }

    parameters {
        //复选框 选择式构建
        gitParameter branchFilter: 'origin.*/(dev|sit|uat|prod)', defaultValue: 'dev', name: 'branchName', type: 'PT_BRANCH'
    }

    // # 定时构建支持
    // triggers {
    //     pollSCM('H/5 * * * 1-5')
    // }

    stages {
        stage('Clean') {
            steps {
                script{
                    // tools.PrintMes("清除工作空间","green")
                    cleanWs()
                    if ("${runOpts}" != "GitlabPush"){
                        utils.GetBuildUser()
                        currentBuild.description = "Manual trigger by ${userName} ${branchName}"
                    }
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
                    sh """
                        sed -i '/<finalName>/d' ${POM_FILE_PATH}
                        sed -i '/<build>/a <finalName>${JOB_NAME}-${branchName}</finalName>' ${POM_FILE_PATH}
                    """
                    buildShell = 'clean package -Dmaven.test.skip'
                    utils.Build(buildType,buildShell)
                }
            }
        }
        
        // CD stages
        stage('Deopy DEV') {
            when {
                environment name: 'branchName', value: 'dev'
            }
            steps {
                sh '''
                     ssh ${SSH_OPTS} -i ${DEV_SSH_KEY} -p ${DEV_SSH_PORT}  ${DEV_SSH_USER}@${DEV_SSH_HOST} 'ls -lha /tmp/' \
                     && scp ${SSH_OPTS} -i ${DEV_SSH_KEY} -r -P ${DEV_SSH_PORT} target/${JOB_NAME}*.jar ${DEV_SSH_USER}@${DEV_SSH_HOST}:/tmp/

                     if [ "${buildType}" == 'mvn' ];then
                        scp ${SSH_OPTS} -i ${DEV_SSH_KEY} -r -P ${DEV_SSH_PORT} scripts/*.sh ${DEV_SSH_USER}@${DEV_SSH_HOST}:/tmp/
                     fi

                     ssh ${SSH_OPTS} -i ${DEV_SSH_KEY} -p ${DEV_SSH_PORT}  ${DEV_SSH_USER}@${DEV_SSH_HOST} 'ls -lha /tmp/' 
                '''
            }
        }

        stage('Deopy SIT') {
            when {
                environment name: 'branchName', value: 'sit'
            }
            steps {
                println "branch: ${branchName}"
            }
        }

        stage('Deopy UAT') {
            when {
                environment name: 'branchName', value: 'uat'
            }
            steps {
                timeout(15) {
                    script {
                        input message:"Are you ready to deploy ${branchName}?",submitter:"admin",ok:"yes"
                    }
                }
                sh "echo UAT"
            }
        }

        stage('Deopy PROD') {
            when {
                environment name: 'branchName', value: 'prod'
            }
            steps{
                timeout(15) {
                    script {
                        input message:"Are you ready to deploy ${branchName}?",submitter:"admin",ok:"yes"
                    }
                }
                sh "echo PROD"
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
    }
}