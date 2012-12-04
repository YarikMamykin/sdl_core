#include "AppMgr/Application_v1.h"
#include "LoggerHelper.hpp"

namespace NsAppManager
{
    /**
     * \brief Class constructor
     * \param name application name
     * \param connectionId id of the connection associated with this application
     * \param sessionId id of the session associated with this application
     */
    Application_v1::Application_v1(const std::string& name , unsigned int connectionId, unsigned char sessionId)
        :Application(name, connectionId, sessionId, 1)
    {
        LOG4CPLUS_INFO_EXT(mLogger, " Created an application " << name << " for the connection id " << connectionId << " session id " << (uint)sessionId);
    }

    /**
     * \brief Copy constructor
     */
    Application_v1::Application_v1( const Application_v1& app )
        :Application(app)
    {
    }

    /**
     * \brief Default class destructor
     */
    Application_v1::~Application_v1( )
    {
        LOG4CPLUS_INFO_EXT(mLogger, " Deleted an application " << mName << " connection id " << mConnectionID << " session id " << mSessionID);
    }

    /**
     * \brief retrieve does the application use vehicle data
     * \return does the application use vehicle data
     */
    bool Application_v1::getUsesVehicleData( ) const
    {
        return m_bUsesVehicleData;
    }

    /**
     * \brief Set application usage of vehicle data
     * \param value does the application use vehicle data
     */
    void Application_v1::setUsesVehicleData(bool value)
    {
        m_bUsesVehicleData = value;
    }

    /**
     * \brief retrieve application desired language
     * \return application desired language
     */
    const NsAppLinkRPC::Language& Application_v1::getLanguageDesired( ) const
    {
        return mLanguageDesired;
    }

    /**
     * \brief Set application desired languuage
     * \param value application desired language
     */
    void Application_v1::setLanguageDesired(NsAppLinkRPC::Language value)
    {
        mLanguageDesired = value;
    }

    /**
     * \brief retrieve application HMI desired display language
     * \return application HMI desired display language
     */
    const NsAppLinkRPC::Language &Application_v1::getHMIDisplayLanguageDesired() const
    {
        return mHMIDisplayLanguageDesired;
    }

    /**
     * \brief Set application HMI desired display language
     * \param value application HMI desired display language
     */
    void Application_v1::setHMIDisplayLanguageDesired(NsAppLinkRPC::Language value)
    {
        mHMIDisplayLanguageDesired = value;
    }

    /**
     * \brief retrieve application audio streaming state
     * \return application audio streaming state
     */
    const NsAppLinkRPC::AudioStreamingState& Application_v1::getApplicationAudioStreamingState( ) const
    {
        return mAudioStreamingState;
    }

    /**
     * \brief Set application audio streaming state
     * \param streamingState audio streaming state of application
     */
    void Application_v1::setApplicationAudioStreamingState( const NsAppLinkRPC::AudioStreamingState& streamingState )
    {
        mAudioStreamingState = streamingState;
    }

    /**
     * \brief retrieve application system context
     * \return application system context
     */
    const NsAppLinkRPC::SystemContext &Application_v1::getSystemContext() const
    {
        return mSystemContext;
    }

    /**
     * \brief set application system context
     * \param application system context
     */
    void Application_v1::setSystemContext(NsAppLinkRPC::SystemContext value)
    {
        mSystemContext = value;
    }

}
