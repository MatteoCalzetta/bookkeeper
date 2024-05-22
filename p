[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Detecting the operating system and CPU architecture
[INFO] ------------------------------------------------------------------------
[INFO] os.detected.name: linux
[INFO] os.detected.arch: x86_64
[INFO] os.detected.release: ubuntu
[INFO] os.detected.release.version: 22.04
[INFO] os.detected.release.like.ubuntu: true
[INFO] os.detected.release.like.debian: true
[INFO] os.detected.classifier: linux-x86_64
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO] 
[INFO] Apache BookKeeper :: Parent                                        [pom]
[INFO] Apache BookKeeper :: Native IO Library                             [nar]
[INFO] Apache BookKeeper :: Test Tools                                    [jar]
[INFO] Apache BookKeeper :: Circe Checksum Library                        [nar]
[INFO] Apache BookKeeper :: Server                                        [jar]
[INFO] Apache BookKeeper :: Build Tools                                   [jar]
[INFO] JaCoCo Test Coverage                                               [jar]
[INFO] 
[INFO] ------------------< org.apache.bookkeeper:bookkeeper >------------------
[INFO] Building Apache BookKeeper :: Parent 4.18.0-SNAPSHOT               [1/7]
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Apache BookKeeper :: Parent 4.18.0-SNAPSHOT:
[INFO] 
[INFO] Apache BookKeeper :: Parent ........................ FAILURE [  0.052 s]
[INFO] Apache BookKeeper :: Native IO Library ............. SKIPPED
[INFO] Apache BookKeeper :: Test Tools .................... SKIPPED
[INFO] Apache BookKeeper :: Circe Checksum Library ........ SKIPPED
[INFO] Apache BookKeeper :: Server ........................ SKIPPED
[INFO] Apache BookKeeper :: Build Tools ................... SKIPPED
[INFO] JaCoCo Test Coverage ............................... SKIPPED
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.844 s
[INFO] Finished at: 2024-05-21T16:00:59+02:00
[INFO] ------------------------------------------------------------------------
[ERROR] Unknown lifecycle phase "bookkeeper-server". You must specify a valid lifecycle phase or a goal in the format <plugin-prefix>:<goal> or <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>. Available lifecycle phases are: validate, initialize, generate-sources, process-sources, generate-resources, process-resources, compile, process-classes, generate-test-sources, process-test-sources, generate-test-resources, process-test-resources, test-compile, process-test-classes, test, prepare-package, package, pre-integration-test, integration-test, post-integration-test, verify, install, deploy, pre-clean, clean, post-clean, pre-site, site, post-site, site-deploy. -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/LifecyclePhaseNotFoundException
