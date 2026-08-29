package net.matsudamper.gptclient.entity

sealed interface ImageAttachmentValidation {
    data object Valid : ImageAttachmentValidation

    data object ImageNotSupported : ImageAttachmentValidation

    data class TooManyImages(
        val maxImageCount: Int,
    ) : ImageAttachmentValidation
}

fun ChatGptModel.validateImageAttachment(imageCount: Int): ImageAttachmentValidation {
    if (imageCount == 0) {
        return ImageAttachmentValidation.Valid
    }
    if (!enableImage) {
        return ImageAttachmentValidation.ImageNotSupported
    }
    if (imageCount > maxImageCount) {
        return ImageAttachmentValidation.TooManyImages(maxImageCount)
    }
    return ImageAttachmentValidation.Valid
}

fun ImageAttachmentValidation.errorMessage(): String? {
    return when (this) {
        ImageAttachmentValidation.Valid -> null
        ImageAttachmentValidation.ImageNotSupported -> "このモデルは画像に対応していません"
        is ImageAttachmentValidation.TooManyImages -> "このモデルでは画像は${maxImageCount}枚までです"
    }
}

fun <T> ChatGptModel.selectableImages(
    currentCount: Int,
    newSelections: List<T>,
): Pair<List<T>, ImageAttachmentValidation> {
    if (newSelections.isEmpty()) {
        return newSelections to ImageAttachmentValidation.Valid
    }
    if (!enableImage) {
        return listOf<T>() to ImageAttachmentValidation.ImageNotSupported
    }
    val remainingCount = maxImageCount - currentCount
    if (remainingCount <= 0) {
        return listOf<T>() to ImageAttachmentValidation.TooManyImages(maxImageCount)
    }
    val acceptedSelections = newSelections.take(remainingCount)
    val validation = if (newSelections.size > acceptedSelections.size) {
        ImageAttachmentValidation.TooManyImages(maxImageCount)
    } else {
        ImageAttachmentValidation.Valid
    }
    return acceptedSelections to validation
}

fun isImageAttachmentAllowed(
    model: ChatGptModel?,
    imageCount: Int,
): Boolean {
    if (imageCount == 0) {
        return true
    }
    if (model == null) {
        return false
    }
    return model.validateImageAttachment(imageCount) is ImageAttachmentValidation.Valid
}
