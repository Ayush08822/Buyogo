CREATE TABLE machine_events (
    event_id VARCHAR(50) PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL,
    line_id VARCHAR(50),
    factory_id VARCHAR(50),
    event_time TIMESTAMP NOT NULL,
    received_time TIMESTAMP NOT NULL,
    duration_ms INT NOT NULL,
    defect_count INT NOT NULL,
    payload_hash INT -- Optional: used for quick identical payload comparison
);