variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "프로젝트 이름 (리소스 이름 prefix)"
  type        = string
  default     = "nbe-team02"
}

# ──────────────────────────────────────────
# DB
# ──────────────────────────────────────────
variable "db_name" {
  description = "PostgreSQL 데이터베이스 이름"
  type        = string
  default     = "gamedb"
}

variable "db_username" {
  description = "PostgreSQL 유저명"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "PostgreSQL 비밀번호"
  type        = string
  sensitive   = true
}

# ──────────────────────────────────────────
# 애플리케이션 환경변수
# ──────────────────────────────────────────
variable "jwt_secret_key" {
  description = "JWT 시크릿 키"
  type        = string
  sensitive   = true
}

variable "igdb_client_id" {
  description = "IGDB Client ID"
  type        = string
  sensitive   = true
}

variable "igdb_client_secret" {
  description = "IGDB Client Secret"
  type        = string
  sensitive   = true
}

variable "steam_api_key" {
  description = "Steam API Key"
  type        = string
  sensitive   = true
}

variable "discord_webhook_url" {
  description = "Discord Webhook URL"
  type        = string
  sensitive   = true
  default     = ""
}

# ──────────────────────────────────────────
# EC2 / Docker
# ──────────────────────────────────────────
variable "key_pair_name" {
  description = "EC2 SSH 키 페어 이름 (AWS에 미리 등록되어 있어야 함)"
  type        = string
}

variable "docker_image" {
  description = "GHCR Docker 이미지 URL (예: ghcr.io/org/repo:latest)"
  type        = string
}
