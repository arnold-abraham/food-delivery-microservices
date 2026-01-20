-- Create per-service databases owned by the same role 'food'
-- This will run only on first container initialization
CREATE DATABASE userdb OWNER food TEMPLATE template1;
CREATE DATABASE restaurantdb OWNER food TEMPLATE template1;
CREATE DATABASE orderdb OWNER food TEMPLATE template1;
CREATE DATABASE deliverydb OWNER food TEMPLATE template1;

-- Optional: grant privileges if needed
GRANT ALL PRIVILEGES ON DATABASE userdb TO food;
GRANT ALL PRIVILEGES ON DATABASE restaurantdb TO food;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO food;
GRANT ALL PRIVILEGES ON DATABASE deliverydb TO food;
