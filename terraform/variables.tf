variable "region" {
  default = "eu-central-1"
}

variable "cluster_name" {
  default = "food-delivery"
}

variable "eks_version" {
  default = "1.30"
}

variable "node_instance_type" {
  # m7i-flex.large: 2 vCPU, 8 GB RAM, x86_64, Free Tier eligible in eu-central-1.
  # t3.medium would be ideal but requires the account Free Tier restriction to be lifted.
  default = "m7i-flex.large"
}

variable "node_count" {
  default = 2
}