# ──────────────────────────────────────────
# GitHub Actions OIDC 연동
# GitHub Actions가 AWS 자격증명 없이 IAM Role을 직접 assume
# Static Access Key 불필요
# ──────────────────────────────────────────

# GitHub OIDC Provider (AWS 계정당 1개)
# 이미 존재하면 import 후 data source로 교체:
#   terraform import aws_iam_openid_connect_provider.github \
#     arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com
resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = ["sts.amazonaws.com"]

  # GitHub OIDC 인증서 thumbprint
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd",
  ]

  tags = { Name = "github-actions-oidc" }
}

# ──────────────────────────────────────────
# GitHub Actions용 IAM Role
# 이 레포지토리의 워크플로우만 assume 가능
# ──────────────────────────────────────────
resource "aws_iam_role" "github_actions" {
  name = "${var.project_name}-github-actions-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRoleWithWebIdentity"
      Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          # 이 레포지토리의 모든 브랜치/태그/PR에서만 assume 가능
          "token.actions.githubusercontent.com:sub" = "repo:prgrms-be-devcourse/NBE8-10-3-Team02:*"
        }
      }
    }]
  })

  tags = { Name = "${var.project_name}-github-actions-role" }
}

# ──────────────────────────────────────────
# GitHub Actions 최소 권한 정책
# ECS Task Definition 업데이트에 필요한 것만 허용
# ──────────────────────────────────────────
resource "aws_iam_role_policy" "github_actions_ecs" {
  name = "${var.project_name}-github-actions-ecs-policy"
  role = aws_iam_role.github_actions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecs:DescribeTaskDefinition",
          "ecs:RegisterTaskDefinition",
        ]
        Resource = ["*"]
      },
      {
        Effect   = "Allow"
        Action   = ["iam:PassRole"]
        Resource = [aws_iam_role.ecs_execution.arn]
      }
    ]
  })
}

output "github_actions_role_arn" {
  description = "GitHub Actions OIDC Role ARN (deploy.yml에 입력)"
  value       = aws_iam_role.github_actions.arn
}
