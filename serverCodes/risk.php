<?php
// Gets number of collisions in an area

$latitude = $_GET['latitude'];
$longitude = $_GET['longitude'];

$latitude = floatval($latitude);
$longitude = floatval($longitude);
if (!is_float($latitude) || !is_float($longitude)) {
    echo "You need to specify a valid latitude and longitude\n";
    exit;
}

$conn = pg_connect("host=localhost dbname=database user=username password=password");

if (!$conn) {
    var_dump(pg_last_error());
    echo "An error occurred.\n";
    exit;
}


//PETROL

$query = "select count(*) from crashes where st_contains(st_expand(GeomFromEWKT('SRID=4326; POINT($longitude $latitude)'),0.009),geom)";

$result =   pg_query($conn, $query);

if (!$result) {
    var_dump(pg_last_error());
    echo "An error occurred.\n";
    exit;
}

$rows = [];
while ($row = pg_fetch_assoc($result)) {
    $rows[] = $row;
}

// Sort by descending distance overall
echo json_encode($rows[0]);
