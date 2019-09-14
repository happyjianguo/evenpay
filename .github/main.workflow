workflow "Build my code" {
  on = "push"
  resolves = ["build image"]
}

action "Docker Login" {
  uses = "actions/docker/login@master"
  secrets = ["haodiaodemingzi", "king1984td21"]
  env = {
    DOCKER_REGISTRY_URL = "docker.pkg.github.com"
  }
}

action "build image" {
  uses = "docker://isuper/java-oracle:jdk_8"
  runs = "python"
  args = "action_build.py"
}
