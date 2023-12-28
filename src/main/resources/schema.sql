DROP TABLE IF EXISTS post;
CREATE TABLE post (
    id bigint auto_increment primary key,
    title varchar(255) not null,
    creator varchar(255) not null,
    created_at datetime default CURRENT_TIMESTAMP not null
);