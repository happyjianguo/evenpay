provider "aws" {
  region = "ap-east-1"
  access_key = "AKIAZ2NPWXFG7WLQU75F"
  secret_key = "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"
}

##################################################################
# Data sources to get VPC, subnet, security group and AMI details
##################################################################
resource "aws_vpc" "default" {
  default = true
}

resource "aws_subnet_id" "all" {
  vpc_id = data.aws_vpc.default.id
}

resource "aws_security_group" "evenpay_sg"{
  name        = "evenpay-sg"
  description = "Security group for evenpay service"
  vpc_id      = data.aws_vpc.default.id
  ingress_cidr_blocks = ["0.0.0.0/0"]
  ingress_rules       = ["http-80-tcp", "all-icmp"]
  egress_rules        = ["all-all"]
}

resource "aws_instance" "evenpay_prod"{
  instance_count = 1 
  name          = "evenpay-prod"
  ami           = "ami-312c845f"
  instance_type = "t2.xlarge"
  subnet_id     = tolist(data.aws_subnet_ids.all.ids)[0]
  //  private_ips                 = ["172.31.32.5", "172.31.46.20"]
  vpc_security_group_ids      = [module.security_group.this_security_group_id]
  associate_public_ip_address = true

  root_block_device = [
    {
      volume_type = "gp2"
      volume_size = 50
    },
  ]

  tags = {
    "Env"      = "Private"
    "Location" = "Secret"
  }
}
