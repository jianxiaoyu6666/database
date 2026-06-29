DELIMITER $$
CREATE PROCEDURE sp_search_user(
    IN p_username VARCHAR(50),
    IN p_real_name VARCHAR(50),
    IN p_user_type VARCHAR(20),
    IN p_status INT
)
BEGIN
SELECT * FROM `user`
WHERE (p_username IS NULL OR username LIKE CONCAT('%', p_username, '%'))
  AND (p_real_name IS NULL OR real_name LIKE CONCAT('%', p_real_name, '%'))
  AND (p_user_type IS NULL OR user_type = p_user_type)
  AND (p_status IS NULL OR status = p_status)
ORDER BY id ASC;
END$$
DELIMITER ;