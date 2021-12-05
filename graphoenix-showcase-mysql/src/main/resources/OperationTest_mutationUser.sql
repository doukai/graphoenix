INSERT INTO
  `user` (
    `user`.`login`,
    `user`.`password`,
    `user`.`name`,
    `user`.`sex`
  )
VALUES
  ('login1', 'password1', :name, :sex) ON DUPLICATE KEY
UPDATE
  `user`.`login` =
VALUES
(`user`.`login`),
  `user`.`password` =
VALUES
(`user`.`password`),
  `user`.`name` =
VALUES
(`user`.`name`),
  `user`.`sex` =
VALUES
(`user`.`sex`);
SET
  @user_id_0_0 = LAST_INSERT_ID();
INSERT INTO
  `user` (
    `user`.`id`,
    `user`.`above_id`,
    `user`.`name`,
    `user`.`version`,
    `user`.`is_deprecated`
  )
VALUES
  (
    JSON_EXTRACT(:organizationInput, '$.id'),
    JSON_EXTRACT(:organizationInput, '$.aboveId'),
    JSON_EXTRACT(:organizationInput, '$.name'),
    JSON_EXTRACT(:organizationInput, '$.version'),
    JSON_EXTRACT(:organizationInput, '$.isDeprecated')
  ) ON DUPLICATE KEY
UPDATE
  `user`.`id` =
VALUES
(`user`.`id`),
  `user`.`above_id` =
VALUES
(`user`.`above_id`),
  `user`.`name` =
VALUES
(`user`.`name`),
  `user`.`version` =
VALUES
(`user`.`version`),
  `user`.`is_deprecated` =
VALUES
(`user`.`is_deprecated`);
INSERT INTO
  `user` (
    `user`.`id`,
    `user`.`name`,
    `user`.`version`,
    `user`.`is_deprecated`
  )
VALUES
  (
    SELECT
      *
    FROM
      JSON_TABLE(
        :roles,
        '$[*]' COLUMNS (
          `id` INT PATH '$.id',
          `name` VARCHAR (255) PATH '$.name',
          `version` INT PATH '$.version',
          `is_deprecated` BOOL PATH '$.isDeprecated'
        )
      ) AS roles
  ) ON DUPLICATE KEY
UPDATE
  `user`.`id` =
VALUES
(`user`.`id`),
  `user`.`name` =
VALUES
(`user`.`name`),
  `user`.`version` =
VALUES
(`user`.`version`),
  `user`.`is_deprecated` =
VALUES
(`user`.`is_deprecated`);
DELETE FROM
  `user_phones`
WHERE
  `user_phones`.`user_id` = @user_id_0_0;
INSERT INTO
  `user_phones` (`user_phones`.`user_id`, `user_phones`.`phone`)
VALUES
  (
    SELECT
      @user_id_0_0,
      `phones`
    FROM
      JSON_TABLE(
        :phones,
        '$[*]' COLUMNS (`phones` VARCHAR (255) PATH '$')
      ) AS phones
  ) ON DUPLICATE KEY
UPDATE
  `user_phones`.`user_id` =
VALUES
(`user_phones`.`user_id`),
  `user_phones`.`phone` =
VALUES
(`user_phones`.`phone`);
SELECT
  JSON_OBJECT(
    'user',
    JSON_EXTRACT(
      (
        SELECT
          JSON_OBJECT(
            'isDeprecated',
            user_1.`is_deprecated`,
            'sex',
            user_1.`sex`,
            'phones',
            JSON_EXTRACT(
              (
                SELECT
                  JSON_ARRAYAGG(user_phones_1.`phone`)
                FROM
                  `user_phones` AS user_phones_1
                WHERE
                  user_phones_1.`user_id` = user_1.`id`
              ),
              '$'
            ),
            'login',
            user_1.`login`,
            'version',
            user_1.`version`,
            'organizationId',
            user_1.`organization_id`,
            'password',
            user_1.`password`,
            'disable',
            user_1.`disable`,
            'name',
            user_1.`name`,
            'id',
            user_1.`id`,
            'age',
            user_1.`age`
          )
        FROM
          `user` AS user_1
        WHERE
          user_1.`id` = @user_id_0_0
        LIMIT
          0, 1
      ), '$'
    )
  ) AS `data`
FROM
  dual