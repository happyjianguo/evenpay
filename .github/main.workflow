workflow "Build my code" {
  on = "push"
  resolves = ["Docker Login", "build image"]
}

action "Docker Login" {
  uses = "actions/docker/login@master"
  secrets = ["haodiaodemingzi", "king1984td21"]
  env = {
    DOCKER_REGISTRY_URL = "index.docker.io/v1/"
  }
}

action "Build image" {
  uses = "docker://isuper/java-oracle:jdk_8"
  runs = "python"
  args = "action_build.py"
}
