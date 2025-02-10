package com.example.myproject;

public record Message(String id, String sender,String groupId, String content){
                
}

//public boolean validate(){
//    return recipients!=null && !recipients.isEmpty() &&
//            sender!=null && !sender.isEmpty() &&
//            message!=null && !message.isEmpty();
//}