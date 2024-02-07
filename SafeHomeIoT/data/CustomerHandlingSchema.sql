DROP DATABASE IF EXISTS CustomerHandling;
CREATE DATABASE CustomerHandling;
USE CustomerHandling;

DROP TABLE IF EXISTS Subscription;
CREATE TABLE Subscription (
    id INT AUTO_INCREMENT,
    note VARCHAR(50),
    status VARCHAR(30),
    startDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(id)
    /*FOREIGN KEY (customerId) REFERENCES Customer(id)*/
);

DROP TABLE IF EXISTS Customer;
CREATE TABLE Customer (
    id INT AUTO_INCREMENT,
    firstName VARCHAR(30),
    lastName VARCHAR(30),
    birthDate TIMESTAMP,
    postalCode VARCHAR(100),
    street VARCHAR(100),
    district VARCHAR(100),
    council VARCHAR(100),
    parish VARCHAR(100),
    email VARCHAR(50),
    password VARCHAR(30),
    doorNumber INT,
    PRIMARY KEY(ID)
);

DROP TABLE IF EXISTS DeviceType;
CREATE TABLE DeviceType (
    id INT AUTO_INCREMENT,
    name VARCHAR(30),
    cost INT,
    PRIMARY KEY(id)
);

DROP TABLE IF EXISTS Device;
CREATE TABLE Device (
    SIMCARD INT,
    MSISDN INT,
    customerId INT,
    deviceTypeId INT,
    status VARCHAR(30),
    PRIMARY KEY (SIMCARD),
    FOREIGN KEY (customerId) REFERENCES Customer(id),
    FOREIGN KEY (deviceTypeId) REFERENCES  DeviceType(id)
);

DROP TABLE IF EXISTS Service;
CREATE TABLE Service (
      id INT AUTO_INCREMENT,
      name VARCHAR(50),
      cost INT,
      PRIMARY KEY (id)
);


DROP TABLE IF EXISTS SubscriptionServices;
CREATE TABLE SubscriptionServices (
      subscriptionId INT,
      serviceId INT,
      PRIMARY KEY (subscriptionId, serviceId),
      FOREIGN KEY (subscriptionId) REFERENCES Subscription(id),
      FOREIGN KEY (serviceId) REFERENCES Service(id)
);

DROP TABLE  IF EXISTS  CustomerSubscriptions;
CREATE TABLE CustomerSubscriptions (
    customerId INT,
    subscriptionId INT,
    PRIMARY KEY (customerId),
    FOREIGN KEY (customerId) REFERENCES Customer(id),
    FOREIGN KEY (subscriptionId) REFERENCES Subscription(id)
);

INSERT INTO Service (name, cost) VALUES ('House Security', 30);
INSERT INTO Service (name, cost) VALUES ('Inventory Management', 20);

Insert INTO DeviceType (name, cost) VALUES ('temperature', 5);
Insert INTO DeviceType (name, cost) VALUES ('motion', 7);
Insert INTO DeviceType (name, cost) VALUES ('smoke', 8);
Insert INTO DeviceType (name, cost) VALUES ('image', 10);
Insert INTO DeviceType (name, cost) VALUES ('video', 20);