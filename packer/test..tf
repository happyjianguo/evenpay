# Specify the provider and access details
provider "aws" {
    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region     = "${var.aws_region}"
}


data "aws_instance" "foo" {
    filter {
        name   = "image-id"
        values = ["ami-xxxxxxxx"]
    }
    filter {
        name   = "tag:Name"
        values = ["evenpay"]
    }
    instance_type = "t2.medinum"
    security_groups = "",
    public_ip = true,
    

}