output "api_server_public_ip" {
  description = "API 서버 고정 IP (Elastic IP)"
  value       = aws_eip.api.public_ip
}

output "rds_endpoint" {
  description = "RDS 엔드포인트 (VPC 내부에서만 접근 가능)"
  value       = aws_db_instance.postgres.address
}

output "rds_port" {
  description = "RDS 포트"
  value       = aws_db_instance.postgres.port
}

output "ecs_cluster_name" {
  description = "ECS 클러스터 이름 (수동 배치 실행 시 사용)"
  value       = aws_ecs_cluster.batch.name
}

output "ecs_task_definition" {
  description = "ECS Task Definition ARN (수동 배치 실행 시 사용)"
  value       = aws_ecs_task_definition.batch.arn
}

output "batch_log_group" {
  description = "CloudWatch 배치 로그 그룹"
  value       = aws_cloudwatch_log_group.batch.name
}

# 수동으로 배치 실행하는 CLI 명령어 출력
output "manual_batch_run_command" {
  description = "배치를 즉시 수동 실행하는 AWS CLI 명령어"
  value       = <<-CMD
    aws ecs run-task \
      --cluster ${aws_ecs_cluster.batch.name} \
      --task-definition ${aws_ecs_task_definition.batch.family} \
      --launch-type FARGATE \
      --network-configuration "awsvpcConfiguration={subnets=[${aws_subnet.public_a.id}],securityGroups=[${aws_security_group.fargate.id}],assignPublicIp=ENABLED}" \
      --region ${var.aws_region}
  CMD
}
