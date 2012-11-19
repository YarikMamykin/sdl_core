#include "../src/../include/JSONHandler/RPC2Objects/NsRPC2Communication/UI/OnAppDeactivated.h"
#include "../src/../include/JSONHandler/RPC2Objects/Marshaller.h"

/*
  interface	NsRPC2Communication::UI
  version	1.2
  generated at	Mon Nov 19 12:18:27 2012
  source stamp	Mon Nov 19 10:17:20 2012
  author	robok0der
*/

using namespace NsRPC2Communication::UI;


OnAppDeactivated& OnAppDeactivated::operator =(const OnAppDeactivated& c)
{
  appName=c.appName;
  return *this;
}


OnAppDeactivated::~OnAppDeactivated(void)
{
}


OnAppDeactivated::OnAppDeactivated(void) : 
  RPC2Notification(Marshaller::METHOD_NSRPC2COMMUNICATION_UI__ONAPPDEACTIVATED)
{
}


OnAppDeactivated::OnAppDeactivated(const OnAppDeactivated& c) : RPC2Notification(Marshaller::METHOD_NSRPC2COMMUNICATION_UI__ONAPPDEACTIVATED)
{
  *this=c;
}


const std::string& OnAppDeactivated::get_appName(void)
{
  return appName;
}

bool OnAppDeactivated::set_appName(const std::string& appName_)
{
  appName=appName_;
  return true;
}

bool OnAppDeactivated::checkIntegrity(void)
{
  return OnAppDeactivatedMarshaller::checkIntegrity(*this);
}
