/****************************************************************************
**
** Copyright (C) 2013 Digia Plc and/or its subsidiary(-ies).
** Contact: http://www.qt-project.org/legal
**
** This file is part of the examples of the Qt Toolkit.
**
** $QT_BEGIN_LICENSE:BSD$
** You may use this file under the terms of the BSD license as follows:
**
** "Redistribution and use in source and binary forms, with or without
** modification, are permitted provided that the following conditions are
** met:
**   * Redistributions of source code must retain the above copyright
**     notice, this list of conditions and the following disclaimer.
**   * Redistributions in binary form must reproduce the above copyright
**     notice, this list of conditions and the following disclaimer in
**     the documentation and/or other materials provided with the
**     distribution.
**   * Neither the name of Digia Plc and its Subsidiary(-ies) nor the names
**     of its contributors may be used to endorse or promote products derived
**     from this software without specific prior written permission.
**
**
** THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
** "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
** LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
** A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
** OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
** SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
** LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
** DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
** THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
** (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
** OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
**
** $QT_END_LICENSE$
**
****************************************************************************/

#include "fboinsgrenderer.h"
#include <QtGui/QOpenGLFramebufferObject>
#include <QtQuick/QQuickWindow>
#include <qsgsimpletexturenode.h>

LogoInFboRenderer::LogoInFboRenderer()
	: aview_("WebViewInQML", false, QSize(512, 512))
{
	// SGEXP
	QMetaObject::invokeMethod(QAndroidOnUiThreadDispatcher::instance(), "test", Qt::AutoConnection, Q_ARG(int, 666));

	qDebug()<<__FUNCTION__<<"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"<<"Constructing...";
	connect(&aview_, SIGNAL(updated()), this, SLOT(textureUpdated()));
#if 0 // SGEXP
	aview_.waitForViewCreation();
	aview_.initializeGL();
	// aview_.setFillColor(Qt::red);
	aview_.loadUrl("http://www.android.com/intl/en/about/");
#endif
}

void LogoInFboRenderer::render()
{
	if (aview_.isCreated() && !aview_.isIntialized())
	{
		aview_.initializeGL();
		aview_.loadUrl("http://www.android.com/intl/en/about/");
	}
	aview_.paintGL(0, 0, aview_.size().width(), aview_.size().height(), true);
	update();
}

QOpenGLFramebufferObject * LogoInFboRenderer::createFramebufferObject(const QSize &size)
{
	qDebug()<<__FUNCTION__<<"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"<<size;
	QOpenGLFramebufferObjectFormat format;
	format.setSamples(4);
	aview_.resize(size);
	return new QOpenGLFramebufferObject(size, format);
}

void LogoInFboRenderer::textureUpdated()
{
	qDebug()<<__FUNCTION__<<"!!!!!!!!!!!!! ***** !!!!!!!!!!!!! ***** !!!!!!!!!!!!!! **** !!!!!!!!!!!!!!";
	invalidateFramebufferObject();
}

QQuickFramebufferObject::Renderer *FboInSGRenderer::createRenderer() const
{
    return new LogoInFboRenderer();
}