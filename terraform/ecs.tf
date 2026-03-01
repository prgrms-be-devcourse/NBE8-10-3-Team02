# ──────────────────────────────────────────
# ECS Cluster
# ──────────────────────────────────────────
resource "aws_ecs_cluster" "batch" {
  name = "${var.project_name}-batch"

  tags = { Name = "${var.project_name}-batch" }
}

# ──────────────────────────────────────────
# CloudWatch Logs (배치 실행 로그)
# ──────────────────────────────────────────
resource "aws_cloudwatch_log_group" "batch" {
  name              = "/ecs/${var.project_name}-batch"
  retention_in_days = 14

  tags = { Name = "${var.project_name}-batch-logs" }
}

# ──────────────────────────────────────────
# IAM - ECS Task Execution Role
# 이미지 pull, CloudWatch 로그 쓰기 권한
# ──────────────────────────────────────────
resource "aws_iam_role" "ecs_execution" {
  name = "${var.project_name}-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ──────────────────────────────────────────
# ECS Task Definition (Batch)
# 같은 Docker 이미지, batch 프로파일로 실행
# ──────────────────────────────────────────
resource "aws_ecs_task_definition" "batch" {
  family                   = "${var.project_name}-batch"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 1024 # 1 vCPU
  memory                   = 2048 # 2 GB
  execution_role_arn       = aws_iam_role.ecs_execution.arn

  container_definitions = jsonencode([{
    name  = "batch"
    image = var.docker_image

    # 앱 시작 시 배치 job 자동 실행 후 컨테이너 종료
    # (application-batch.yml: web-application-type: none, job.enabled: true)
    environment = [
      { name = "SPRING_PROFILES_ACTIVE",        value = "prod,batch" },
      { name = "SPRING_DATASOURCE_URL",          value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/${var.db_name}" },
      { name = "SPRING_DATASOURCE_USERNAME",     value = var.db_username },
      { name = "SPRING_DATASOURCE_PASSWORD",     value = var.db_password },
      { name = "JWT_SECRET_KEY",                 value = var.jwt_secret_key },
      { name = "IGDB_CLIENT_ID",                 value = var.igdb_client_id },
      { name = "IGDB_CLIENT_SECRET",             value = var.igdb_client_secret },
      { name = "DISCORD_WEBHOOK_URL",            value = var.discord_webhook_url },
      { name = "STEAM_API_KEY",                  value = var.steam_api_key },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.batch.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])

  tags = { Name = "${var.project_name}-batch-task" }
}

# ──────────────────────────────────────────
# IAM - EventBridge가 ECS Task를 실행할 권한
# ──────────────────────────────────────────
resource "aws_iam_role" "eventbridge" {
  name = "${var.project_name}-eventbridge-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "events.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "eventbridge" {
  name = "${var.project_name}-eventbridge-policy"
  role = aws_iam_role.eventbridge.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ecs:RunTask"]
        # 특정 revision ARN이 아닌 family 전체에 허용
        # CI/CD가 새 revision 등록 시 EventBridge가 권한 오류 없이 실행 가능
        Resource = ["arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:task-definition/${aws_ecs_task_definition.batch.family}:*"]
      },
      {
        Effect   = "Allow"
        Action   = ["iam:PassRole"]
        Resource = [aws_iam_role.ecs_execution.arn]
      }
    ]
  })
}

# ──────────────────────────────────────────
# EventBridge 스케줄
# 매주 월요일 KST 03:00 = UTC 일요일 18:00
# ──────────────────────────────────────────
resource "aws_cloudwatch_event_rule" "batch_schedule" {
  name                = "${var.project_name}-batch-schedule"
  description         = "매주 월요일 KST 03:00 IGDB 동기화 배치 실행"
  schedule_expression = "cron(0 18 ? * SUN *)"

  tags = { Name = "${var.project_name}-batch-schedule" }
}

resource "aws_cloudwatch_event_target" "batch" {
  rule      = aws_cloudwatch_event_rule.batch_schedule.name
  target_id = "${var.project_name}-batch-fargate"
  arn       = aws_ecs_cluster.batch.arn
  role_arn  = aws_iam_role.eventbridge.arn

  ecs_target {
    # revision 없이 family ARN만 지정 → EventBridge가 항상 최신 revision 사용
    # CI/CD에서 새 revision 등록 시 별도 EventBridge 업데이트 불필요
    task_definition_arn = "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:task-definition/${aws_ecs_task_definition.batch.family}"
    task_count          = 1
    launch_type         = "FARGATE"

    network_configuration {
      subnets          = [aws_subnet.public_a.id]
      security_groups  = [aws_security_group.fargate.id]
      assign_public_ip = true # GHCR 이미지 pull + IGDB API 호출에 필요
    }
  }
}
