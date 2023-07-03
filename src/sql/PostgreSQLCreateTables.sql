DROP SEQUENCE IF EXISTS hospital_id_seq CASCADE;
DROP SEQUENCE IF EXISTS museum_id_seq CASCADE;
DROP SEQUENCE IF EXISTS cafeteria_id_seq CASCADE;
DROP SEQUENCE IF EXISTS hotel_id_seq CASCADE;

DROP TABLE IF EXISTS Hospital;
DROP TABLE IF EXISTS Museum;
DROP TABLE IF EXISTS Cafeteria;
DROP TABLE IF EXISTS Hotel;

CREATE SEQUENCE hospital_id_seq;
CREATE SEQUENCE museum_id_seq;
CREATE SEQUENCE cafeteria_id_seq;
CREATE SEQUENCE hotel_id_seq;

CREATE TABLE Hospital(
    id integer DEFAULT nextval('hospital_id_seq'),
    name CHARACTER VARYING,
    city CHARACTER VARYING,
    street CHARACTER VARYING,
    location GEOMETRY,
    CONSTRAINT Hospital_PK PRIMARY KEY(id)
);

CREATE TABLE Museum(
    id integer DEFAULT nextval('museum_id_seq'),
    name CHARACTER VARYING,
    city CHARACTER VARYING,
    street CHARACTER VARYING,
    web CHARACTER VARYING,
    hasToilets BOOLEAN,
    toiletsWheelchair BOOLEAN,
    location GEOMETRY,
    CONSTRAINT Museum_PK PRIMARY KEY(id)
);

CREATE TABLE Cafeteria(
  id integer DEFAULT nextval('cafeteria_id_seq'),
  name CHARACTER VARYING,
  city CHARACTER VARYING,
  street CHARACTER VARYING,
  web CHARACTER VARYING,
  postcode CHARACTER VARYING,
  smoke BOOLEAN,
  location GEOMETRY,
  CONSTRAINT Cafeteria_PK PRIMARY KEY(id)
);

CREATE TABLE Hotel(
    id integer DEFAULT nextval('hotel_id_seq'),
    name CHARACTER VARYING,
    numberRooms CHARACTER VARYING,
    numberStars CHARACTER VARYING,
    web CHARACTER VARYING,
    city CHARACTER VARYING,
    street CHARACTER VARYING,
    telephone CHARACTER VARYING,
    petsAllowed BOOLEAN,
    location GEOMETRY,
    CONSTRAINT Hotel_PK PRIMARY KEY(id)
);

