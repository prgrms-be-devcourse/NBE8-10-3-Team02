terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # 팀 협업 시 tfstate를 S3에 저장 (선택사항)
  # backend "s3" {
  #   bucket         = "nbe-team02-tfstate"
  #   key            = "terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   dynamodb_table = "nbe-team02-tfstate-lock"
  # }
}

provider "aws" {
  region  = var.aws_region
  profile = "default" # aws configure 로 IAM 자격증명 설정 후 사용

  default_tags {
    tags = {
      Project = var.project_name
    }
  }
}

data "aws_caller_identity" "current" {}
