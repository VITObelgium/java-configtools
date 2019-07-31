pipeline {
    agent {
        dockerfile {
          filename 'Dockerfile'
          dir 'docker'
          additionalBuildArgs '--build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g)'
          args '-v ${HOME}/.ssh:${HOME}/.ssh -v ${HOME}/.m2:${HOME}/.m2'
        }
    }
    environment {
    	MVN_SSL_OPTS = '-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true'
    	MVN_DOCLINT_OPTS = 'org.apache.maven.plugins:maven-javadoc-plugin:3.1.0:jar -DadditionalJOption=-Xdoclint:none'
    }
    stages {
    	stage('Build') {
    		steps {
    			sh 'mvn $MVN_SSL_OPTS -DskipTests -B clean package'
    		}
    	}
    	stage('Test') {
    		steps {
    			sh 'mvn $MVN_SSL_OPTS test'
    		}
    	}
    	stage('Deploy snapshot') {
			when {
				branch 'develop'
			}
			steps {
				sh 'mvn $MVN_SSL_OPTS $MVN_DOCLINT_OPTS -DskipTests source:jar deploy -DaltDeploymentRepository=rma.snapshot::default::https://mvn.marvin.vito.local/repository/rma-snapshot'
			}
    	}
    	stage('Deploy release') {
			when {
				branch 'master'
			}
			steps {
				sh 'mvn $MVN_SSL_OPTS $MVN_DOCLINT_OPTS -DskipTests source:jar deploy -DaltDeploymentRepository=rma.release::default::https://mvn.marvin.vito.local/repository/rma'
			}
    	}
     }
}
