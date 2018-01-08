import groovy.json.JsonSlurper

def call() {
    return new PipelineUtils()
}

class PipelineUtils {

    private static final String PIPELINE_SCRIPTS_STASH_NAME = 'pipeline_scripts'

    String stageNameToDirName(stageName) {
        if (stageName != null) {
            return stageName.toLowerCase().replace(' ', '-')
        }
        return null
    }

    void stashScripts(final context) {
        context.stash name: PIPELINE_SCRIPTS_STASH_NAME, includes: 'h2o-3/scripts/jenkins/groovy/*', allowEmpty: false
    }

    void unstashScripts(final context) {
        context.unstash name: PIPELINE_SCRIPTS_STASH_NAME
    }

    List<String> readSupportedHadoopDistributions(final context, final String buildinfoPath) {
        final List<String> DOCKERIZED_DISTRIBUTIONS = ['cdh', 'hdp']

        context.sh "sed -i 's/SUBST_BUILD_TIME_MILLIS/\"SUBST_BUILD_TIME_MILLIS\"/g' ${buildinfoPath}"
        context.sh "sed -i 's/SUBST_BUILD_NUMBER/\"SUBST_BUILD_NUMBER\"/g' ${buildinfoPath}"

        def jsonStr = readFile(buildinfoPath)
        def buildinfo = new JsonSlurper().parseText(jsonStr)

        def distributionsToBuild = []

        for (distSpec in buildinfo.hadoop_distributions) {
            def distributionStr = distSpec.distribution.toLowerCase()
            for (dockerizedDist in DOCKERIZED_DISTRIBUTIONS) {
                if (distributionStr.startsWith(dockerizedDist)) {
                    def distributionName = dockerizedDist
                    def distributionVersion = distributionStr.replaceFirst(dockerizedDist, '')
                    distributionsToBuild += [
                            name: distributionName,
                            version: distributionVersion
                    ]
                    echo "Supported dist found: dist: ${distributionName}, ver: ${distributionVersion}"
                }
            }
        }

        return distributionsToBuild
    }
}

return this