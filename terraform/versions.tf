terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # Uncomment after creating the bucket/table yourself. Not created here because
  # this request did not include a state backend.
  #
  # backend "s3" {
  #   bucket         = "your-terraform-state-bucket"
  #   key            = "ias-academy/ecs/terraform.tfstate"
  #   region         = "ap-south-1"
  #   dynamodb_table = "your-terraform-locks"
  #   encrypt        = true
  # }
}
