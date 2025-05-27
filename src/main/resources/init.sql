CREATE TABLE IF NOT EXISTS users (
                                     chat_id BIGINT PRIMARY KEY,
                                     username VARCHAR(255),
    state VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS responses (
                                         id BIGSERIAL PRIMARY KEY,
                                         user_id BIGINT,
                                         name VARCHAR(255),
    email VARCHAR(255),
    rating INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(chat_id)
    );