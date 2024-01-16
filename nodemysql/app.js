const express = require("express");
const mysql = require("mysql");
var url = require("url")
var body = require("body-parser")

const app = express();

//Creating a connection and connecting to the database
const db = mysql.createConnection({
    host: "localhost",
    user: "root",
    password: "",
    database: "sms_database"
  });

  db.connect((err) => {
    if (err) {
      throw err;
    }
    console.log("Connection to Database is done");
  });


// sendSMS API: Creates a new record that contains phone number and body as arguments. Returns a status code (1:OK/0:Not OK)
var q;
app.get("/sendSMS", function (req, res) {
    q = url.parse(req.url, true).query;
    //var sql = "INSERT INTO sms_table (phone_no, Body) VALUES (";
    var sql = "INSERT INTO `sms_table` (`phone_no`, `Body`) VALUES (";
    sql = sql +"'"  + q.phone + "' , '" + q.message+"'" + ")";
    db.query(sql, function (err, result) {
    if (err) {
        res.end("0");
        throw err
    };
    console.log("1 record inserted");
    res.end("1");
    });
    });

    app.get("/", (req, res) => {
          res.send("1");
       });


// sendSMS by using POST - The API gets a JASON object in the body

app.use(body.json());
app.post('/sendSMS', function (req, res) {
    console.log(req.body); 

    //JSON.parse('{"phone_no":"01234567899", "Body":"Demo message"}');
    var obj = req.body;

    if(obj.phone_no==undefined || obj.Body==undefined)
    {
        res.end("0");
        console.log("phone number or Body entered is undefined, Did not save SMS"); 
        return
    }

    var sql = "INSERT INTO `sms_table` (`phone_no`, `Body`) VALUES (";
    sql = sql +"'"  + obj.phone_no + "' , '" + obj.Body+"'" + ")";
    db.query(sql, function (err, result) {
    if (err) {
        res.end("0");
        throw err
    };
    console.log("1 record inserted");
    res.end("1");
    
    });
});

//getSMS API: we will get the oldest unsent message and set its sent value to 1, indicating that it is sent, This is not actually correct as the API could be called but the SMS could fail to be sent.

app.get('/getSMS',function(req, res){   
    //SELECT `phone_no`,`Body`,`sent` FROM `sms_table` WHERE `sent`=0 HAVING MIN(`msg_recived_time`);
    var query_statement = 'SELECT * FROM `sms_table` WHERE `sent`=0 HAVING MIN(`msg_recived_time`)';
    var oldest_id;
    db.query(query_statement, (err, result) => {
        if (err) throw err;
        var x = result.length;
        console.log("x is "+x);
        if(x == 0)
        {
            console.log('Table is empty');
            //res.status(404);
            res.send('Table empty');
            return;
        }
        
        if (result[0].sent != 0)
        {
            console.log("Error !! Sent is not 0");
            return;
        }
        //res.end=result[0];
        oldest_id = result[0].ID;
        console.log(result[0]);
        res.send(result[0]);
        // console.log('Oldest id is '+oldest_id);
        
        query_statement = 'UPDATE `sms_table` SET `sent`= 1 WHERE `ID`=';
        query_statement = query_statement + oldest_id;
        //console.log(query_statement);

        db.query(query_statement, (err, result) => {
            if (err) throw err;

        });
      });
      console.log("getSMS has been called");
    

});

  
app.listen("3000", () => {
    console.log("Server is successfully running on port 3000");
  });