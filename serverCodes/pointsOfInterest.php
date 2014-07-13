<?php
// Gets n of each type of points of interest in an area

$latitude = $_GET['latitude'];
$longitude = $_GET['longitude'];

$latitude = floatval($latitude);
$longitude = floatval($longitude);
if (!is_float($latitude) || !is_float($longitude)) {
    echo "You need to specify a valid latitude and longitude\n";
    exit;
}

// Taken from http://stackoverflow.com/questions/10053358/measuring-the-distance-between-two-coordinates-in-php
// Function licensed under cc-wiki with attribution required
// Author martinstoeckli
function vincentyGreatCircleDistance(
    $latitudeFrom, $longitudeFrom, $latitudeTo, $longitudeTo, $earthRadius = 6371000)
{
    // convert from degrees to radians
    $latFrom = deg2rad($latitudeFrom);
    $lonFrom = deg2rad($longitudeFrom);
    $latTo = deg2rad($latitudeTo);
    $lonTo = deg2rad($longitudeTo);

    $lonDelta = $lonTo - $lonFrom;
    $a = pow(cos($latTo) * sin($lonDelta), 2) +
        pow(cos($latFrom) * sin($latTo) - sin($latFrom) * cos($latTo) * cos($lonDelta), 2);
    $b = sin($latFrom) * sin($latTo) + cos($latFrom) * cos($latTo) * cos($lonDelta);

    $angle = atan2(sqrt($a), $b);
    return $angle * $earthRadius;
}

function get_result_rows($result) {
    if (!$result) {
        var_dump(pg_last_error());
        echo "An error occurred.\n";
        exit;
    }

    $rows = [];
    while ($row = pg_fetch_assoc($result)) {
        $rows[] = $row;
    }
    return $rows;
}

$conn = pg_connect("host=localhost dbname=database user=username password=password");

if (!$conn) {
    var_dump(pg_last_error());
    echo "An error occurred.\n";
    exit;
}


//PETROL
$petrolStations = get_result_rows(pg_query($conn, "select 'PETROL' as type, name,st_y(geom) as lat, st_x(geom) as lon,st_distance(GeomFromEWKT('SRID=4326; POINT(".$longitude." ".$latitude.")'),geom ) as distance from osm_petrol order by distance asc limit 10;"));

//REST
$restStops = get_result_rows(pg_query($conn, "select 'REST' as type,rest_are_1 as name, st_y(geom) as lat, st_x(geom) as lon,st_distance(GeomFromEWKT('SRID=4326; POINT(".$longitude." ".$latitude.")'),Geography(geom) ) as distance from restareas order by distance asc limit 10;"));

//BBQ
$BBQs = get_result_rows(pg_query($conn, "select 'BBQ' as type, '' as description, st_y(geom) as lat, st_x(geom) as lon,st_distance(GeomFromEWKT('SRID=4326; POINT(".$longitude." ".$latitude.")'),geom ) as distance from bbq_view order by distance asc limit 10;"));

//TOILET
$TOILET = get_result_rows(pg_query($conn, "select 'TOILET' as type,name as description, st_y(geom) as lat, st_x(geom) as lon,st_distance(GeomFromEWKT('SRID=4326; POINT(".$longitude." ".$latitude.")'),geom ) as distance from toilet order by distance asc limit 10;"));

//PARK
$PARK = get_result_rows(pg_query($conn, "select 'PARK' as type, name as description, st_y(st_closestpoint(geom,GeomFromEWKT('SRID=4326; POINT(".$longitude." ".$latitude.")'))) as lat, st_x(st_closestpoint(geom,GeomFromEWKT('SRID=4326; POINT(".$longitude." ".$latitude.")'))) as lon,st_distance(GeomFromEWKT('SRID=4326; POINT(".$longitude." ".$latitude.")'),geom ) as distance from playground order by distance asc limit 10;"));

// This approach for combining two SQL queries is ugly as sin, but it was quick to implement.
$combined = array_merge($petrolStations, $restStops, $BBQs,$TOILET,$PARK);

for ($i = 0; $i < count($combined); $i++) {
    $combined[$i]['lat'] = floatval($combined[$i]['lat']);
    $combined[$i]['lon'] = floatval($combined[$i]['lon']);
}

for ($i = 0; $i < count($combined); $i++) {
    $element = $combined[$i];
    $combined[$i]['distance'] = vincentyGreatCircleDistance($latitude, $longitude, $element['lat'], $element['lon']);
}


// Sort by descending distance overall
usort($combined, function($a, $b) { return floatval($a['distance']) - floatval($b['distance']); });


echo json_encode($combined);