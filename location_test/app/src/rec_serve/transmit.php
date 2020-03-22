<?php
    // 接受定位程序（安卓手机app）位置数据请求，并转发给后台tornado web服务。
    // 该transmit服务相当于lbs，负载均衡服务
    $logFile = ".\loc.transmit." . date("Ymd") . ".log";
    $ct = date("Y-m-d H:i:s", time());
    
    // echo "转发地址信息<br>";
    // header("Location: http://localhost:8080/location?lat=1&long=2&target=abc")
    echo 'lat:'.$_GET['lat'].'<br/>';
    echo 'long:'.$_GET['long'].'<br/>';
    echo 'target:'.$_GET['target'].'<br/>';
    
    $url = "http://localhost:8080/location";    // python tornado web server to handle location info
    $get_info = 'get loc info :lat:'.$_GET['lat'].'long:'.$_GET['long'].'target:'.$_GET['target']."request to :".$url;
    error_log("[" . $ct . "] ".$get_info." \r\n", 3, $logFile);
    $url = $url.'?lat='.$_GET['lat'].'&long='.$_GET['long'].'&target='.$_GET['target'];
    echo 'request:'.$url;
    echo '<br/>';
    // $file = fopen("http://localhost:8080/location?lat=1&long=2&target=abc", "r");
    echo 'response: ';
    $file = fopen($url, "r");
    $line = "";
    while (!feof($file)) {
        $line = $line.fgets($file, 1024);
        // echo $line;
    }
    echo $line;
    fclose($file);
    echo "<br>转发成功<br>";
    error_log("[" . $ct . "] get response:" .$line." \r\n", 3, $logFile);
    return "result:".$line;
?>