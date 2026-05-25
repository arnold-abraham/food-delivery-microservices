output "cluster_name" {
  value = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.main.endpoint
}

output "ecr_registry" {
  value       = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com"
  description = "ECR registry base URL — prefix every image tag with this"
}

output "kubeconfig_command" {
  value       = "aws eks update-kubeconfig --region ${var.region} --name ${var.cluster_name}"
  description = "Run this to point kubectl at the cluster"
}

output "estimated_hourly_cost_usd" {
  value       = "~$0.25  (EKS $0.10 + 2×t3.medium $0.084 + NAT GW $0.048 + EBS)"
  description = "Rough cost while cluster is running — destroy when done"
}