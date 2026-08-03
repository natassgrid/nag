-- ============================================================
-- Geo-location lookup tables: country → state → city
-- ============================================================
-- These tables provide cascading dropdown data for the UI.
-- In production, replace the seed data below with a full DB dump
-- from the countries-states-cities-database project.
-- Schema: examination_service (co-located with exam centre data)

-- ── Country ─────────────────────────────────────────────────────────────────
CREATE TABLE examination_service.geo_country (
    id          BIGINT       PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    iso2        VARCHAR(2),
    iso3        VARCHAR(3),
    phone_code  VARCHAR(20),
    capital     VARCHAR(100),
    currency    VARCHAR(50),
    region      VARCHAR(100),
    subregion   VARCHAR(100),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_geo_country_name ON examination_service.geo_country(name);

-- ── State ───────────────────────────────────────────────────────────────────
CREATE TABLE examination_service.geo_state (
    id           BIGINT       PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    country_id   BIGINT       NOT NULL REFERENCES examination_service.geo_country(id),
    state_code   VARCHAR(10),
    type         VARCHAR(50),   -- "state" / "union territory"
    active       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_geo_state_country ON examination_service.geo_state(country_id);
CREATE INDEX idx_geo_state_name    ON examination_service.geo_state(name);

-- ── City ────────────────────────────────────────────────────────────────────
CREATE TABLE examination_service.geo_city (
    id           BIGINT       PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    state_id     BIGINT       NOT NULL REFERENCES examination_service.geo_state(id),
    country_id   BIGINT       NOT NULL REFERENCES examination_service.geo_country(id),
    latitude     NUMERIC(10,8),
    longitude    NUMERIC(11,8),
    active       BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_geo_city_state   ON examination_service.geo_city(state_id);
CREATE INDEX idx_geo_city_country ON examination_service.geo_city(country_id);
CREATE INDEX idx_geo_city_name    ON examination_service.geo_city(name);

-- ── Update examination_centre to use geo FK IDs ─────────────────────────────
ALTER TABLE examination_service.examination_centre
    ADD COLUMN country_id BIGINT REFERENCES examination_service.geo_country(id),
    ADD COLUMN state_id   BIGINT REFERENCES examination_service.geo_state(id),
    ADD COLUMN city_id    BIGINT REFERENCES examination_service.geo_city(id);

-- ════════════════════════════════════════════════════════════════════════════
-- SEED DATA: India + 28 states/8 UTs + major cities
-- In production this will be replaced by a full DB dump.
-- ════════════════════════════════════════════════════════════════════════════

-- Country: India (id matches the open-source countries-states-cities DB)
INSERT INTO examination_service.geo_country (id, name, iso2, iso3, phone_code, capital, currency, region, subregion)
VALUES (101, 'India', 'IN', 'IND', '91', 'New Delhi', 'INR', 'Asia', 'Southern Asia');

-- States & Union Territories
INSERT INTO examination_service.geo_state (id, name, country_id, state_code, type) VALUES
(4023, 'Andhra Pradesh',       101, 'AP', 'state'),
(4024, 'Arunachal Pradesh',    101, 'AR', 'state'),
(4025, 'Assam',                101, 'AS', 'state'),
(4026, 'Bihar',                101, 'BR', 'state'),
(4027, 'Chhattisgarh',         101, 'CG', 'state'),
(4028, 'Goa',                  101, 'GA', 'state'),
(4029, 'Gujarat',              101, 'GJ', 'state'),
(4030, 'Haryana',              101, 'HR', 'state'),
(4031, 'Himachal Pradesh',     101, 'HP', 'state'),
(4032, 'Jharkhand',            101, 'JH', 'state'),
(4033, 'Karnataka',            101, 'KA', 'state'),
(4034, 'Kerala',               101, 'KL', 'state'),
(4035, 'Madhya Pradesh',       101, 'MP', 'state'),
(4036, 'Maharashtra',          101, 'MH', 'state'),
(4037, 'Manipur',              101, 'MN', 'state'),
(4038, 'Meghalaya',            101, 'ML', 'state'),
(4039, 'Mizoram',              101, 'MZ', 'state'),
(4040, 'Nagaland',             101, 'NL', 'state'),
(4041, 'Odisha',               101, 'OD', 'state'),
(4042, 'Punjab',               101, 'PB', 'state'),
(4043, 'Rajasthan',            101, 'RJ', 'state'),
(4044, 'Sikkim',               101, 'SK', 'state'),
(4045, 'Tamil Nadu',           101, 'TN', 'state'),
(4046, 'Telangana',            101, 'TS', 'state'),
(4047, 'Tripura',              101, 'TR', 'state'),
(4048, 'Uttar Pradesh',        101, 'UP', 'state'),
(4049, 'Uttarakhand',          101, 'UK', 'state'),
(4050, 'West Bengal',          101, 'WB', 'state'),
-- Union Territories
(4051, 'Delhi',                         101, 'DL', 'union territory'),
(4052, 'Jammu and Kashmir',             101, 'JK', 'union territory'),
(4053, 'Ladakh',                        101, 'LA', 'union territory'),
(4054, 'Chandigarh',                    101, 'CH', 'union territory'),
(4055, 'Puducherry',                    101, 'PY', 'union territory'),
(4056, 'Andaman and Nicobar Islands',   101, 'AN', 'union territory'),
(4057, 'Dadra and Nagar Haveli and Daman and Diu', 101, 'DD', 'union territory'),
(4058, 'Lakshadweep',                   101, 'LD', 'union territory');

-- Cities (major cities for dev/testing — ~60 cities across states)
INSERT INTO examination_service.geo_city (id, name, state_id, country_id, latitude, longitude) VALUES
-- Delhi
(50001, 'New Delhi',       4051, 101, 28.61390000, 77.20900000),
-- Maharashtra
(50002, 'Mumbai',          4036, 101, 19.07600000, 72.87770000),
(50003, 'Pune',            4036, 101, 18.52040000, 73.85670000),
(50004, 'Nagpur',          4036, 101, 21.14660000, 79.08880000),
(50005, 'Nashik',          4036, 101, 19.99740000, 73.79050000),
-- Karnataka
(50006, 'Bengaluru',       4033, 101, 12.97160000, 77.59460000),
(50007, 'Mysuru',          4033, 101, 12.29580000, 76.63940000),
(50008, 'Hubli',           4033, 101, 15.36470000, 75.12400000),
-- Tamil Nadu
(50009, 'Chennai',         4045, 101, 13.08250000, 80.27500000),
(50010, 'Coimbatore',      4045, 101, 11.01680000, 76.95580000),
(50011, 'Madurai',         4045, 101, 9.93920000,  78.12770000),
-- Telangana
(50012, 'Hyderabad',       4046, 101, 17.38500000, 78.48670000),
(50013, 'Warangal',        4046, 101, 17.97840000, 79.60000000),
-- Kerala
(50014, 'Thiruvananthapuram', 4034, 101, 8.52410000,  76.93610000),
(50015, 'Kochi',           4034, 101, 9.93120000,  76.26730000),
-- Gujarat
(50016, 'Ahmedabad',       4029, 101, 23.02250000, 72.57140000),
(50017, 'Surat',           4029, 101, 21.17020000, 72.83110000),
(50018, 'Vadodara',        4029, 101, 22.30720000, 73.18120000),
-- Rajasthan
(50019, 'Jaipur',          4043, 101, 26.91240000, 75.78730000),
(50020, 'Jodhpur',         4043, 101, 26.28930000, 73.02440000),
(50021, 'Udaipur',         4043, 101, 24.58540000, 73.71250000),
-- Uttar Pradesh
(50022, 'Lucknow',         4048, 101, 26.84670000, 80.94620000),
(50023, 'Noida',           4048, 101, 28.53550000, 77.39100000),
(50024, 'Varanasi',        4048, 101, 25.31760000, 82.98740000),
(50025, 'Agra',            4048, 101, 27.17670000, 78.00810000),
(50026, 'Kanpur',          4048, 101, 26.44990000, 80.33190000),
(50027, 'Prayagraj',       4048, 101, 25.43580000, 81.84630000),
-- Madhya Pradesh
(50028, 'Bhopal',          4035, 101, 23.25990000, 77.41260000),
(50029, 'Indore',          4035, 101, 22.71960000, 75.85770000),
-- Bihar
(50030, 'Patna',           4026, 101, 25.61000000, 85.14140000),
-- West Bengal
(50031, 'Kolkata',         4050, 101, 22.57260000, 88.36390000),
(50032, 'Siliguri',        4050, 101, 26.72710000, 88.39530000),
-- Punjab
(50033, 'Chandigarh',      4054, 101, 30.73330000, 76.77940000),
(50034, 'Ludhiana',        4042, 101, 30.90100000, 75.85730000),
(50035, 'Amritsar',        4042, 101, 31.63400000, 74.87230000),
-- Haryana
(50036, 'Gurugram',        4030, 101, 28.45950000, 77.02660000),
(50037, 'Faridabad',       4030, 101, 28.40890000, 77.31780000),
-- Jharkhand
(50038, 'Ranchi',          4032, 101, 23.34410000, 85.30960000),
(50039, 'Jamshedpur',      4032, 101, 22.80460000, 86.20290000),
-- Chhattisgarh
(50040, 'Raipur',          4027, 101, 21.25140000, 81.62960000),
-- Odisha
(50041, 'Bhubaneswar',     4041, 101, 20.29610000, 85.82450000),
-- Andhra Pradesh
(50042, 'Visakhapatnam',   4023, 101, 17.68680000, 83.21850000),
(50043, 'Vijayawada',      4023, 101, 16.50620000, 80.64800000),
(50044, 'Tirupati',        4023, 101, 13.63880000, 79.42060000),
-- Assam
(50045, 'Guwahati',        4025, 101, 26.14450000, 91.73620000),
-- Uttarakhand
(50046, 'Dehradun',        4049, 101, 30.31650000, 78.03220000),
-- Goa
(50047, 'Panaji',          4028, 101, 15.49900000, 73.82780000);
