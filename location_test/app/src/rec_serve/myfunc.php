<?php

header("Content-Type: application/json; charset=UTF-8");

// 轨迹展示页面js调用该php，该php直接查询sqlite数据库位置数据
function query_position() {
    
    class MyDB extends SQLite3
   {
      function __construct()
      {
         $this->open('G:\ASProjects\location_test\app\src\rec_serve\locationManage.db');
      }
   }
   $db = new MyDB();
   if(!$db){
      echo $db->lastErrorMsg();
   } else {
      // echo "Opened database successfully<br>";
   }
   // query 
   $sql =<<<EOF
      SELECT * from location;
EOF;
   $ret = $db->query($sql);
   $outp = array();
   while($row = $ret->fetchArray(SQLITE3_ASSOC) ){
      /*echo "ID = ". $row['ID'] . "<br>";
      echo "lat = ". $row['lat'] ."<br>";
      echo "long = ". $row['long'] ."<br>";
      echo "target =  ".$row['target'] ."<br>";
      echo "recv_time =  ".$row['recv_time'] ."<br><br>";*/
      // echo json_encode($row);
      array_push($outp,$row);
      //echo "<br>";
   }
    // echo json_encode($outp);
   // $myJSON = json_encode($outp);
   echo json_encode($outp);
   // echo "get reponse";
   // echo "Operation done successfully<br>";
   $db->close();
   
   // return ;
}
query_position();
?>