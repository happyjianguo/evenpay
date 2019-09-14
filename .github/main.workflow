workflow "Build my code" {
  on = "push"
  resolves = ["build image"]
}

action "build image" {
  uses = "docker://isuper/java-oracle:jdk_8"
  runs = "python"
  args = "action_build.py"
}
