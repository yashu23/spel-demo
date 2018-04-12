var apiResponse = input.get('apiResponse');
var responseBody = apiResponse.get('responseBody').get('NCDStatus');

if (apiResponse.get('responseStatusCode') == '500') {
  rr.add("NCD_UNID_FAILED");
  rrText.add("NCD Test not executed");
}

if (apiResponse.get('responseStatusCode') == '400') {
  rr.add("NCD_UNID_FAILED_1");
  rrText.add("NCD Test not executed");
}

if(responseBody.get('Device.Ethernet.Interface.1.Enable') == '1') {
  rr.add("UNID PORT LOCKED");
  rrText.add("NCD Test not executed as port is locked");
}

if(responseBody.get('Device.Ethernet.Interface.1.Status') == 'Down') {
  rr.add("UNID OPERATIONAL STATE DOWN");
  rrText.add("NCD Test not executed as operational state is down");
}

if(size(rr) == 0) {
  result = "3";
  rr.add("NCD_SUCCESS");
  rrText.add("NCD Test executed successfully");
}

return result;