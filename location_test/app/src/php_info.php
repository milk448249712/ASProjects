<?php
    // phpinfo();
	//$var_a = $_GET["para"];
	//echo($var_a);
	$input = file_get_contents("php://input");
	$jsonData = json_decode($input);
	/*foreach (getallheaders() as $name => $value) { 
	echo "$name: $value\n"; }*/
	//echo($input);
	echo($jsonData->lat);
	/*$myfile = fopen("newfile.txt", "a+") or die("Unable to open file!");
	fwrite($myfile, $input."\n");
	fclose($myfile);*/
?>