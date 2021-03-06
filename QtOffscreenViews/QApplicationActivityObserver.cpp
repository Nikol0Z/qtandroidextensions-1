/*
  Offscreen Android Views library for Qt

  Author:
  Sergey A. Galin <sergey.galin@gmail.com>

  Distrbuted under The BSD License

  Copyright (c) 2014, DoubleGIS, LLC.
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

  * Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
  * Neither the name of the DoubleGIS, LLC nor the names of its contributors
    may be used to endorse or promote products derived from this software
    without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
  THE POSSIBILITY OF SUCH DAMAGE.
*/

#include <QtCore/QCoreApplication>
#include <QtCore/QDebug>
#include "QApplicationActivityObserver.h"


QApplicationActivityObserver * QApplicationActivityObserver::instance()
{
	static QMutex mymutex;
	static QScopedPointer<QApplicationActivityObserver> instance;
	QMutexLocker locker(&mymutex);

	// Creating instance
	if (instance.isNull())
	{
		instance.reset(new QApplicationActivityObserver());
	}

	// Installing event filter
	static bool installed_filter = false;
	if (!installed_filter)
	{
		QCoreApplication * app = QCoreApplication::instance();
		if (app)
		{
			app->installEventFilter(instance.data());
			installed_filter = true;
			qDebug()<<__FUNCTION__<<"Successfully installed event filter.";
		}
		else
		{
			qWarning()<<__FUNCTION__<<"installing event filter failed because QCoreApplication instance doesn't exist, will try later.";
		}
	}

	return instance.data();
}

void QApplicationActivityObserver::setApplicationActive(bool active)
{
	if (active != is_active_)
	{
		qDebug()<<"QApplicationActivityObserver::setApplicationActive"<<active;
		is_active_ = active;
		emit applicationActiveStateChanged();
	}
}

bool QApplicationActivityObserver::eventFilter(QObject * obj, QEvent * evnt)
{
	if (evnt->type() == QEvent::ApplicationActivate)
	{
		QMetaObject::invokeMethod(this, "setApplicationActive", Qt::QueuedConnection, Q_ARG(bool,true));
	}
	else if (evnt->type() == QEvent::ApplicationDeactivate)
	{
		setApplicationActive(false);
	}
	return QObject::eventFilter(obj, evnt);
}

