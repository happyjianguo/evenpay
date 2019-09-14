workflow "Build my code" {
  on = "push"
  resolves = ["Docker Login", "Build image"]
}

action "Docker Login" {
  uses = "actions/docker/login@master"
  secrets = ["DOCKER_USERNAME", "DOCKER_PASSWORD"]
  env = {
    DOCKER_REGISTRY_URL = "index.docker.io/v1/"
  }
}

action "Build image" {
  needs = ["Docker Login"]
  uses = "docker://isuper/java-oracle:jdk_8"
  runs = "python"
  args = "action_build.py"
}
