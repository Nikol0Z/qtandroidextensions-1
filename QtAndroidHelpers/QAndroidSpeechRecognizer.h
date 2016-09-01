/*
	Lightweight access to various Android APIs for Qt

	Author:
	Sergey A. Galin <sergey.galin@gmail.com>

	Distrbuted under The BSD License

	Copyright (c) 2016, DoubleGIS, LLC.
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

#pragma once
#include <QtCore/QMap>
#include <QtCore/QObject>
#include <QtCore/QString>
#include <QtCore/QTimer>
#include <QJniHelpers.h>


// SpeechRecognizer wrapper.
// Note: this class is QML-singleton friendly.
class QAndroidSpeechRecognizer
	: public QObject
{
	Q_OBJECT
	Q_PROPERTY(bool listening READ listening NOTIFY listeningChanged)
	Q_PROPERTY(float rmsdB READ rmsdB NOTIFY rmsdBChanged)
	Q_PROPERTY(int permissionRequestCode READ permissionRequestCode WRITE setPermissionRequestCode NOTIFY permissionRequestCodeChanged)
public:
	QAndroidSpeechRecognizer(QObject * p = 0);
	virtual ~QAndroidSpeechRecognizer();

	static void preloadJavaClasses();
	static bool isRecognitionAvailableStatic();

	bool listening() const { return listening_; }
	float rmsdB() const { return rmsdB_; }

	static const int
		ANDROID_SPEECHRECOGNIZER_ERROR_NETWORK_TIMEOUT = 1,
		ANDROID_SPEECHRECOGNIZER_ERROR_NETWORK = 2,
		ANDROID_SPEECHRECOGNIZER_ERROR_AUDIO = 3,
		ANDROID_SPEECHRECOGNIZER_ERROR_SERVER = 4,
		ANDROID_SPEECHRECOGNIZER_ERROR_CLIENT = 5,
		ANDROID_SPEECHRECOGNIZER_ERROR_SPEECH_TIMEOUT = 6,
		ANDROID_SPEECHRECOGNIZER_ERROR_NO_MATCH = 7,
		ANDROID_SPEECHRECOGNIZER_ERROR_RECOGNIZER_BUSY = 8,
		ANDROID_SPEECHRECOGNIZER_ERROR_INSUFFICIENT_PERMISSIONS = 9;

	static const int
		ANDROID_RECOGNIZERINTENT_RESULT_NO_MATCH = 1,
		ANDROID_RECOGNIZERINTENT_RESULT_CLIENT_ERROR = 2,
		ANDROID_RECOGNIZERINTENT_RESULT_SERVER_ERROR = 3,
		ANDROID_RECOGNIZERINTENT_RESULT_NETWORK_ERROR = 4,
		ANDROID_RECOGNIZERINTENT_RESULT_AUDIO_ERROR = 5;

	static const QString
		ANDROID_RECOGNIZERINTENT_ACTION_GET_LANGUAGE_DETAILS,
		ANDROID_RECOGNIZERINTENT_ACTION_RECOGNIZE_SPEECH,
		ANDROID_RECOGNIZERINTENT_ACTION_VOICE_SEARCH_HANDS_FREE,
		ANDROID_RECOGNIZERINTENT_ACTION_WEB_SEARCH,

		ANDROID_RECOGNIZERINTENT_DETAILS_META_DATA,

		ANDROID_RECOGNIZERINTENT_EXTRA_CALLING_PACKAGE,
		ANDROID_RECOGNIZERINTENT_EXTRA_CONFIDENCE_SCORES,
		ANDROID_RECOGNIZERINTENT_EXTRA_LANGUAGE,
		ANDROID_RECOGNIZERINTENT_EXTRA_LANGUAGE_MODEL,
		ANDROID_RECOGNIZERINTENT_EXTRA_LANGUAGE_PREFERENCE,
		ANDROID_RECOGNIZERINTENT_EXTRA_MAX_RESULTS,
		ANDROID_RECOGNIZERINTENT_EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,
		ANDROID_RECOGNIZERINTENT_EXTRA_ORIGIN,
		ANDROID_RECOGNIZERINTENT_EXTRA_PARTIAL_RESULTS,
		ANDROID_RECOGNIZERINTENT_EXTRA_PREFER_OFFLINE,
		ANDROID_RECOGNIZERINTENT_EXTRA_PROMPT,
		ANDROID_RECOGNIZERINTENT_EXTRA_RESULTS,
		ANDROID_RECOGNIZERINTENT_EXTRA_RESULTS_PENDINGINTENT,
		ANDROID_RECOGNIZERINTENT_EXTRA_RESULTS_PENDINGINTENT_BUNDLE,
		ANDROID_RECOGNIZERINTENT_EXTRA_SECURE,
		ANDROID_RECOGNIZERINTENT_EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
		ANDROID_RECOGNIZERINTENT_EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
		ANDROID_RECOGNIZERINTENT_EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
		ANDROID_RECOGNIZERINTENT_EXTRA_SUPPORTED_LANGUAGES,
		ANDROID_RECOGNIZERINTENT_EXTRA_WEB_SEARCH_ONLY,

		ANDROID_RECOGNIZERINTENT_LANGUAGE_MODEL_FREE_FORM,
		ANDROID_RECOGNIZERINTENT_LANGUAGE_MODEL_WEB_SEARCH,

		ANDROID_SPEECHRECOGNIZER_RESULTS_RECOGNITION;

public slots:
	// SpeechRecognier functions
	bool isRecognitionAvailable() const;
	bool startListening(const QString & action);
	void stopListening();
	void cancel();

	// Filling in extra parameters
	void clearExtras() { string_extras_.clear(); bool_extras_.clear(); int_extras_.clear(); enable_timeout_timer_ = false; }
	void addStringExtra(const QString & key, const QString & value) { string_extras_.insert(key, value); }
	void addBoolExtra(const QString & key, bool value) { bool_extras_.insert(key, value); }
	void addIntExtra(const QString & key, int value) { int_extras_.insert(key, value); }


	// Higher-level functions

	void startListeningFreeForm();
	void startListeningWebSearch();
	void startListeningHandsFree();

	void extraSetPrompt(const QString & prompt);
	void extraSetLanguage(const QString & ietf_language);
	void extraSetMaxResults(int results);
	void extraSetPartialResults();

	// Set voice input timeout parameters.
	// Please note that the normal setting of these parameters to SpeechRecognizer does not work
	// on 4.3 (Jelly Bean) and up.To make timeout work on all versions of Android please set
	// timer_workaround_ms > 0. Note that it also enables partial results.
	void extraSetListeningTimeouts(
		int min_phrase_length_ms
		, int possibly_complete_ms
		, int complete_ms
		, int timer_workaround_ms);

	int permissionRequestCode() const;
	void setPermissionRequestCode(int code);

signals:
	void listeningChanged(bool listening);
	void beginningOfSpeech();
	void endOfSpeech();
	void error(int code, QString message);
	void partialResults(const QStringList & results);
	void partialResult(const QString & last_result);
	void readyForSpeech();
	void results(const QStringList & results, bool secure);
	void result(const QString & last_result, bool secure);
	void rmsdBChanged(float rmsdb);
	void permissionRequestCodeChanged(int code);

private slots:
	void javaOnBeginningOfSpeech();
	void javaOnEndOfSpeech();
	void javaOnError(int code);
	void javaOnPartialResults(const QStringList & results);
	void javaOnReadyForSpeech();
	void javaOnResults(const QStringList & results, bool secure);
	void javaOnRmsdBChanged(float rmsdb);
	void onTimeoutTimerTimeout();

private:
	void listeningStopped();
	QString errorCodeToMessage(int code);

	friend Q_DECL_EXPORT void JNICALL Java_QAndroidSpeechRecognizer_nativeOnBeginningOfSpeech(JNIEnv *, jobject, jlong param);
	friend Q_DECL_EXPORT void JNICALL Java_QAndroidSpeechRecognizer_nativeOnEndOfSpeech(JNIEnv *, jobject, jlong param);
	friend Q_DECL_EXPORT void JNICALL Java_QAndroidSpeechRecognizer_nativeOnError(JNIEnv *, jobject, jlong param, jint code);
	friend Q_DECL_EXPORT void JNICALL Java_QAndroidSpeechRecognizer_nativeOnPartialResults(JNIEnv *, jobject, jlong param, jobject bundle_results);
	friend Q_DECL_EXPORT void JNICALL Java_QAndroidSpeechRecognizer_nativeOnReadyForSpeech(JNIEnv *, jobject, jlong param, jobject bundle_params);
	friend Q_DECL_EXPORT void JNICALL Java_QAndroidSpeechRecognizer_nativeOnResults(JNIEnv *, jobject, jlong param, jobject bundle_results, jboolean secure);
	friend Q_DECL_EXPORT void JNICALL Java_QAndroidSpeechRecognizer_nativeOnRmsChanged(JNIEnv *, jobject, jlong param, jfloat rmsdB);

private:
	bool listening_;
	float rmsdB_;
	QMap<QString, QString> string_extras_;
	QMap<QString, bool> bool_extras_;
	QMap<QString, int> int_extras_;
	QScopedPointer<QJniObject> listener_;
	QTimer timeout_timer_;
	bool enable_timeout_timer_;
	int permission_request_code_;
};


