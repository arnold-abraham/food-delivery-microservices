locals {
  services = toset([
    "api-gateway",
    "user-service",
    "restaurant-service",
    "order-service",
    "payment-service",
    "delivery-service",
  ])
}

resource "aws_ecr_repository" "services" {
  for_each             = local.services
  name                 = each.value
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = false
  }

  # Auto-delete images when the repo is destroyed (keeps the account clean)
  force_delete = true
}