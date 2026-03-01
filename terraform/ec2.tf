# ──────────────────────────────────────────
# Amazon Linux 2023 최신 AMI 자동 조회
# ──────────────────────────────────────────
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

# ──────────────────────────────────────────
# IAM - EC2가 SSM Session Manager를 통해
#        콘솔에서 터미널 접속 가능하게 함
# ──────────────────────────────────────────
resource "aws_iam_role" "ec2" {
  name = "${var.project_name}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${var.project_name}-ec2-profile"
  role = aws_iam_role.ec2.name
}

# ──────────────────────────────────────────
# EC2 t3.small (API 서버)
# Blue-Green: nginx 컨테이너(:80) → blue/green 컨테이너 (proxy 네트워크)
# nginx는 호스트 설치 없이 Docker 컨테이너로 운영
# 첫 배포 시 deploy.yml이 nginx 컨테이너를 자동 기동
# ──────────────────────────────────────────
resource "aws_instance" "api" {
  ami                    = data.aws_ami.amazon_linux_2023.id
  instance_type          = "t3.small"
  key_name               = var.key_pair_name
  subnet_id              = aws_subnet.public_a.id
  vpc_security_group_ids = [aws_security_group.api.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name

  user_data = <<-EOF
    #!/bin/bash
    set -e

    # Docker 설치
    dnf update -y
    dnf install -y docker
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ec2-user
  EOF

  tags = { Name = "${var.project_name}-api" }
}

# ──────────────────────────────────────────
# Elastic IP - 재시작해도 IP 유지
# ──────────────────────────────────────────
resource "aws_eip" "api" {
  instance = aws_instance.api.id
  domain   = "vpc"

  tags = { Name = "${var.project_name}-api-eip" }
}
