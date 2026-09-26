CREATE TABLE auto_services (
    id SERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    price INT NOT NULL CHECK (price >= 0)
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    service_id INTEGER NOT NULL REFERENCES auto_services(id) ON DELETE CASCADE,
    client_name VARCHAR(20) NOT NULL,
    order_date DATE NOT NULL,
    status VARCHAR(15) NOT NULL CHECK (status IN ('GAVE', 'IN_PROGRESS', 'DONE'))
);
