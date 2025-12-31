-- ============================================
-- LOOKUP TABLES (6)
-- ============================================

CREATE TABLE gemstone_types (
                                id SERIAL PRIMARY KEY,
                                name VARCHAR(50) UNIQUE NOT NULL,
                                category VARCHAR(20),                    -- PRECIOUS, SEMI_PRECIOUS
                                is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO gemstone_types (name, category) VALUES
                                                ('Ruby', 'PRECIOUS'),
                                                ('Sapphire', 'PRECIOUS'),
                                                ('Emerald', 'PRECIOUS'),
                                                ('Alexandrite', 'PRECIOUS'),
                                                ('Spinel', 'SEMI_PRECIOUS'),
                                                ('Tourmaline', 'SEMI_PRECIOUS'),
                                                ('Aquamarine', 'SEMI_PRECIOUS'),
                                                ('Garnet', 'SEMI_PRECIOUS'),
                                                ('Other', 'OTHER');



CREATE TABLE colors (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(30) UNIQUE NOT NULL,
                        is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO colors (name) VALUES
                              ('Red'), ('Blue'), ('Pink'), ('Green'),
                              ('Yellow'), ('Orange'), ('Purple'), ('White'), ('Other');

CREATE TABLE color_qualities (
                                 id SERIAL PRIMARY KEY,
                                 name VARCHAR(20) UNIQUE NOT NULL,
                                 rank INT NOT NULL,                       -- ML:  1=best
                                 is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO color_qualities (name, rank) VALUES
                                             ('Royal', 1),
                                             ('Vivid', 2),
                                             ('Normal', 3),
                                             ('Light', 4);

CREATE TABLE clarity_grades (
                                id SERIAL PRIMARY KEY,
                                name VARCHAR(30) UNIQUE NOT NULL,
                                rank INT NOT NULL,                       -- ML: 1=best
                                is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO clarity_grades (name, rank) VALUES
                                            ('Excellent', 1),
                                            ('Very Good', 2),
                                            ('Good', 3),
                                            ('Fair', 4),
                                            ('Poor', 5);

CREATE TABLE cuts (
                      id SERIAL PRIMARY KEY,
                      name VARCHAR(30) UNIQUE NOT NULL,
                      is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO cuts (name) VALUES
                            ('Oval'), ('Round'), ('Cushion'), ('Pear'),
                            ('Emerald'), ('Cabochon'), ('Marquise'), ('Other');

CREATE TABLE origins (
                         id SERIAL PRIMARY KEY,
                         name VARCHAR(50) UNIQUE NOT NULL,
                         is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO origins (name) VALUES
                               ('Sri Lanka'), ('Myanmar (Burma)'), ('Colombia'),
                               ('Thailand'), ('Madagascar'), ('Tanzania'), ('Other');


CREATE TABLE treatments (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(50) UNIQUE NOT NULL,
                            is_active BOOLEAN DEFAULT TRUE
);
INSERT INTO treatments (name) VALUES
                                  ('Unheated'), ('Heated'), ('Minor Heat'), ('Other'), ('Unknown');

-- ============================================
-- CORE TABLES (3)
-- ============================================

CREATE TABLE listings (
                          id BIGSERIAL PRIMARY KEY,
                          seller_id BIGINT NOT NULL REFERENCES users_table(id),

    -- Basic Info
                          title VARCHAR(255) NOT NULL,
                          description TEXT,

    -- The 4 Cs + Extras (Maps to your dataset)
                          gemstone_type_id INT NOT NULL REFERENCES gemstone_types(id),  -- Master_Gem_Type
                          carat_weight DECIMAL(8,3) NOT NULL,                           -- Carat_Weight
                          color_id INT REFERENCES colors(id),                           -- Gem_Color
                          color_quality_id INT REFERENCES color_qualities(id),          -- Color_Quality
                          clarity_id INT REFERENCES clarity_grades(id),                 -- Clarity_Score
                          cut_id INT REFERENCES cuts(id),                               -- Shape
                          origin_id INT REFERENCES origins(id),                         -- Origin
                          treatment_id INT REFERENCES treatments(id),                   -- Treatment

    -- Dimensions (X, Y, Z)
                          length_mm DECIMAL(6,2),                                       -- X
                          width_mm DECIMAL(6,2),                                        -- Y
                          depth_mm DECIMAL(6,2),                                        -- Z

    -- Pricing
                          price DECIMAL(14,2) NOT NULL,
                          currency VARCHAR(3) DEFAULT 'LKR',

    -- Certificate (optional)
                          is_certified BOOLEAN DEFAULT FALSE,
                          certificate_info VARCHAR(255),

    -- Status & Stats
                          status VARCHAR(20) DEFAULT 'ACTIVE',
                          views_count INT DEFAULT 0,
                          favorites_count INT DEFAULT 0,

    -- Sale Outcome (for ML)
                          is_sold BOOLEAN DEFAULT FALSE,
                          sold_price DECIMAL(14,2),
                          sold_at TIMESTAMP,

    -- Timestamps
                          created_at TIMESTAMP DEFAULT NOW(),
                          updated_at TIMESTAMP DEFAULT NOW()
);


CREATE TABLE listing_images (
                                id BIGSERIAL PRIMARY KEY,
                                listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
                                image_url VARCHAR(500) NOT NULL,
                                is_primary BOOLEAN DEFAULT FALSE,
                                display_order INT DEFAULT 0,
                                created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE favorites (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
                           listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
                           created_at TIMESTAMP DEFAULT NOW(),
                           UNIQUE(user_id, listing_id)
);

-- ============================================
-- INDEXES
-- ============================================

CREATE INDEX idx_listings_seller ON listings(seller_id);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_type ON listings(gemstone_type_id);
CREATE INDEX idx_listings_price ON listings(price);
CREATE INDEX idx_listings_carat ON listings(carat_weight);
CREATE INDEX idx_listings_created ON listings(created_at DESC);