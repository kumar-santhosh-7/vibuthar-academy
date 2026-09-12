resource "terraform_data" "existing_db" {
  lifecycle {
    precondition {
      condition = var.create_rds || (
        var.existing_db_url != "" &&
        var.existing_db_username != "" &&
        var.existing_db_password != ""
      )
      error_message = "When create_rds is false, set existing_db_url, existing_db_username, and existing_db_password."
    }
  }
}
